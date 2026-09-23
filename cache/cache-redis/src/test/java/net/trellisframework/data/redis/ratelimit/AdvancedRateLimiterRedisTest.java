package net.trellisframework.data.redis.ratelimit;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Limited;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Op;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the contract against a real Redis (embedded, started on a free port) and adds the Redis-only
 * guarantees: cross-process atomicity, no virtual-thread pinning, wire-format compatibility and latency.
 */
class AdvancedRateLimiterRedisTest extends AdvancedRateLimiterContract {

    private static RedisServer server;
    private static RedissonClient redisson;
    private static RedissonClient secondPod;
    private static String address;

    @BeforeAll
    static void startRedis() {
        try {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            server = RedisServer.newRedisServer().port(port).setting("save \"\"").setting("appendonly no").build();
            server.start();
            address = "redis://127.0.0.1:" + port;
        } catch (Exception e) {
            assumeTrue(false, "embedded Redis could not be started — skipping Redis-backed rate limiter tests: " + e);
        }
        redisson = client();
        secondPod = client();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisson != null) redisson.shutdown();
        if (secondPod != null) secondPod.shutdown();
        if (server != null) server.stop();
    }

    private static RedissonClient client() {
        Config config = new Config();
        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }

    @Override
    protected void wireBackend() {
        redisson.getKeys().flushdb();
        wire(redisson);
    }

    /** 5. Two "pods" (two clients) hammering one key with 100 per window admit at most 100 in total. */
    @Test
    void crossProcessAcquiresNeverExceedLimit() throws Exception {
        String key = "rate-limiter:cross:r1";
        RateLimit limits = RateLimit.builder().second(10, 100).build(); // 10 s window so the burst cannot straddle it
        LongAdder admitted = new LongAdder();
        List<Callable<Void>> work = new ArrayList<>();
        for (RedissonClient pod : List.of(redisson, secondPod))
            for (int t = 0; t < 20; t++)
                work.add(() -> {
                    for (int i = 0; i < 25; i++)
                        if (RateLimiterScript.execute(pod, Op.ACQUIRE, List.of(new Limited(key, limits)), System.currentTimeMillis(), UUID.randomUUID().toString()))
                            admitted.increment();
                    return null;
                });
        runAll(work, Executors.newFixedThreadPool(40));
        assertEquals(100, admitted.sum(), "1000 attempts from two pods must admit exactly the window limit");
    }

    @Test
    void concurrentAcquiresThroughPoolApiNeverExceedLimit() throws Exception {
        AdvancedRateLimiter.<String>pool("burst").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 100).build()).build();
        AtomicInteger admitted = new AtomicInteger();
        List<Callable<Void>> work = new ArrayList<>();
        for (int t = 0; t < 200; t++)
            work.add(() -> {
                for (int i = 0; i < 5; i++)
                    if (AdvancedRateLimiter.tryAcquire("burst") != null) admitted.incrementAndGet();
                return null;
            });
        runAll(work, Executors.newFixedThreadPool(200));
        assertEquals(100, admitted.get());
    }

    /**
     * 6. Virtual threads: 200 acquires on a virtual-thread executor record zero {@code jdk.VirtualThreadPinned}
     * events. A deliberately pinning control run first proves the recording would have caught it.
     */
    @Test
    void acquireOnVirtualThreadsDoesNotPinCarrier() throws Exception {
        if (Runtime.version().feature() < 24)
            assertTrue(pinnedEvents(() -> {
                Object monitor = new Object();
                try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
                    vt.submit(() -> {
                        synchronized (monitor) {
                            Thread.sleep(50); // parks while holding a monitor → pinned
                            return null;
                        }
                    }).get();
                }
            }) > 0, "control: the JFR recording must detect a pinned virtual thread");

        AdvancedRateLimiter.<String>pool("vt").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 1_000_000).maxConcurrent(1_000_000, Duration.ofSeconds(10)).build())
                .targetLimits(TargetLimits.create().defaultLimit(RateLimit.builder().second(10, 1_000_000).build()))
                .build();
        long pinned = pinnedEvents(() -> {
            try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < 200; i++)
                    futures.add(vt.submit(() -> {
                        RateLimitResource<String> r = AdvancedRateLimiter.tryAcquire("vt", "T");
                        assertNotNull(r);
                        assertTrue(r.canAcquire());
                        r.release();
                        return null;
                    }));
                for (Future<?> f : futures) f.get();
            }
        });
        assertEquals(0, pinned, "rate limiter must not pin carrier threads");
    }

    /** A check reports the limit without touching it: expired permits are pruned by acquires, never by CHECK. */
    @Test
    void checkNeverWritesToRedis() {
        String key = "rate-limiter:readonly:r1";
        RateLimit limits = RateLimit.builder().second(10, 5).maxConcurrent(1, Duration.ofMillis(50)).build();
        long now = System.currentTimeMillis();
        assertTrue(RateLimiterScript.execute(redisson, Op.ACQUIRE, List.of(new Limited(key, limits)), now, "p1"));
        assertFalse(RateLimiterScript.execute(redisson, Op.CHECK, List.of(new Limited(key, limits)), now, ""),
                "the only permit is live, so a check must refuse");

        long expired = now + 200;
        assertTrue(RateLimiterScript.execute(redisson, Op.CHECK, List.of(new Limited(key, limits)), expired, ""));
        assertEquals(1, redisson.getScoredSortedSet(key + "#p", StringCodec.INSTANCE).size(),
                "CHECK must leave the permit set exactly as it found it");

        assertTrue(RateLimiterScript.execute(redisson, Op.ACQUIRE, List.of(new Limited(key, limits)), expired, "p2"));
        assertEquals(1, redisson.getScoredSortedSet(key + "#p", StringCodec.INSTANCE).size(),
                "ACQUIRE prunes the expired permit, then records its own");
    }

    /** Windows, permits and cool-off live in native keys next to the logical key, each with its own TTL. */
    @Test
    void stateLivesInNativeKeysWithTtls() {
        String key = "rate-limiter:compat:r1";
        RateLimit limits = RateLimit.builder().second(10, 5).maxConcurrent(3, Duration.ofSeconds(10)).build();
        long now = System.currentTimeMillis();

        assertTrue(RateLimiterScript.execute(redisson, Op.ACQUIRE, List.of(new Limited(key, limits)), now, "p1"));
        assertTrue(RateLimiterScript.execute(redisson, Op.ACQUIRE, List.of(new Limited(key, limits)), now + 1, "p2"));
        assertEquals("2", redisson.<String>getBucket(key + "#w:10000", StringCodec.INSTANCE).get());
        assertEquals(2, redisson.getScoredSortedSet(key + "#p", StringCodec.INSTANCE).size());
        long windowTtl = redisson.getBucket(key + "#w:10000").remainTimeToLive();
        assertTrue(windowTtl > 0 && windowTtl <= 10_000, "window TTL must be the window length, was " + windowTtl);
        long permitTtl = redisson.getScoredSortedSet(key + "#p").remainTimeToLive();
        assertTrue(permitTtl > 60_000 && permitTtl <= 70_000, "permit TTL must be max(window, permit timeout) + 60 s, was " + permitTtl);

        RateLimiterScript.execute(redisson, Op.COOL_OFF, List.of(new Limited(key, limits)), now + 3, "500");
        long coolOffTtl = redisson.getBucket(key + "#c").remainTimeToLive();
        assertTrue(coolOffTtl > 0 && coolOffTtl <= 500, "cool-off TTL must be the cool-off length, was " + coolOffTtl);
        assertTrue(!RateLimiterScript.execute(redisson, Op.CHECK, List.of(new Limited(key, limits)), now + 4, ""), "blocked while cooling off");
    }

    @Test
    void releasingLastPermitDeletesEmptyState() {
        String key = "rate-limiter:empty:r1";
        RateLimit limits = RateLimit.builder().maxConcurrent(1, Duration.ofSeconds(10)).build();
        assertTrue(RateLimiterScript.execute(redisson, Op.ACQUIRE, List.of(new Limited(key, limits)), System.currentTimeMillis(), "only"));
        assertTrue(redisson.getScoredSortedSet(key + "#p").isExists());
        RateLimiterScript.execute(redisson, Op.RELEASE, List.of(new Limited(key, limits)), System.currentTimeMillis(), "only");
        assertTrue(!redisson.getScoredSortedSet(key + "#p").isExists(), "empty permit set must be gone");
    }

    @Test
    void survivesScriptCacheFlush() {
        AdvancedRateLimiter.<String>pool("flush").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(2).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("flush"));
        redisson.getScript().scriptFlush(); // e.g. Redis restarted → NOSCRIPT → script is reloaded transparently
        assertNotNull(AdvancedRateLimiter.tryAcquire("flush"));
        assertNull(AdvancedRateLimiter.tryAcquire("flush"), "state must have survived the reload");
    }

    @Test
    void redisFailureFallsBackToLocalLimits() {
        RedissonClient broken = mock(RedissonClient.class);
        when(broken.getScript(any(org.redisson.client.codec.Codec.class))).thenThrow(new RuntimeException("redis down"));
        AdvancedRateLimiter.reset();
        wire(broken);

        AdvancedRateLimiter.<String>pool("down").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(2).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("down"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("down"));
        assertNull(AdvancedRateLimiter.tryAcquire("down"), "limits must still hold on the local fallback");
    }

    /**
     * Latency against the embedded Redis; prints the numbers for the PR and guards against regressions. Limits
     * mirror production (fixed windows plus a small, expiring permit set): the script cost is O(state size), so an
     * unbounded {@code maxConcurrent} without releases would measure JSON growth, not the limiter.
     * <p>
     * Uncontended p99 is the gate. The 200-thread closed loop saturates the single Redis thread, so its latency
     * is queueing (in-flight / throughput) rather than per-call cost; it is reported as throughput.
     */
    @Test
    void acquireLatencyAndThroughput() throws Exception {
        AdvancedRateLimiter.<String>pool("perf").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(60, 10_000_000).day(10_000_000).maxConcurrent(10_000_000, Duration.ofMillis(1)).build())
                .targetLimits(TargetLimits.create().defaultLimit(RateLimit.builder().second(60, 10_000_000).build()))
                .build();
        for (int i = 0; i < 2_000; i++) // warm-up: JIT, Redisson connection pool, script cache
            assertNotNull(AdvancedRateLimiter.tryAcquire("perf", "T"));

        long[] single = new long[2_000];
        for (int i = 0; i < single.length; i++) {
            long start = System.nanoTime();
            assertNotNull(AdvancedRateLimiter.tryAcquire("perf", "T"));
            single[i] = System.nanoTime() - start;
        }
        java.util.Arrays.sort(single);
        double p50 = single[single.length / 2] / 1e6, p99 = single[(int) (single.length * 0.99)] / 1e6;
        System.out.printf("AdvancedRateLimiter uncontended acquire: p50=%.3f ms p99=%.3f ms%n", p50, p99);

        int threads = 200, perThread = 25;
        long[] samples = new long[threads * perThread];
        List<Callable<Void>> work = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int offset = t * perThread;
            work.add(() -> {
                for (int i = 0; i < perThread; i++) {
                    long start = System.nanoTime();
                    assertNotNull(AdvancedRateLimiter.tryAcquire("perf", "T"));
                    samples[offset + i] = System.nanoTime() - start;
                }
                return null;
            });
        }
        long start = System.nanoTime();
        runAll(work, Executors.newFixedThreadPool(threads));
        double seconds = (System.nanoTime() - start) / 1e9;
        java.util.Arrays.sort(samples);
        System.out.printf("AdvancedRateLimiter %d threads x %d acquires: %.0f acquires/s, p50=%.2f ms p99=%.2f ms max=%.2f ms%n",
                threads, perThread, samples.length / seconds, samples[samples.length / 2] / 1e6,
                samples[(int) (samples.length * 0.99)] / 1e6, samples[samples.length - 1] / 1e6);

        assertTrue(p50 < 5, "uncontended p50 acquire latency regressed: " + p50 + " ms");
        assertTrue(p99 < 25, "uncontended p99 acquire latency regressed: " + p99 + " ms");
        assertTrue(samples.length / seconds > 500, "saturated throughput regressed: " + samples.length / seconds + " acquires/s");
    }

    // ---- helpers -------------------------------------------------------------------------------

    private static void runAll(List<Callable<Void>> work, ExecutorService executor) throws Exception {
        try {
            for (Future<Void> f : executor.invokeAll(work)) f.get();
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private interface Body {
        void run() throws Exception;
    }

    private static long pinnedEvents(Body body) throws Exception {
        Path dump = Files.createTempFile("rate-limiter-vt", ".jfr");
        try (Recording recording = new Recording()) {
            recording.enable("jdk.VirtualThreadPinned").withThreshold(Duration.ZERO).withStackTrace();
            recording.start();
            body.run();
            recording.stop();
            recording.dump(dump);
            List<RecordedEvent> events = RecordingFile.readAllEvents(dump);
            long pinned = events.stream().filter(e -> e.getEventType().getName().equals("jdk.VirtualThreadPinned")).count();
            if (pinned > 0)
                events.stream().filter(e -> e.getEventType().getName().equals("jdk.VirtualThreadPinned")).limit(3)
                        .forEach(e -> System.out.println("pinned: " + e.getStackTrace()));
            return pinned;
        } finally {
            Files.deleteIfExists(dump);
        }
    }
}
