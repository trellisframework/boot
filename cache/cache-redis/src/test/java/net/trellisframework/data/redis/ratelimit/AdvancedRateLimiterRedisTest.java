package net.trellisframework.data.redis.ratelimit;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.redis.ratelimit.RateLimiterStore.Limited;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.BatchOptions;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.context.ApplicationContext;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
 * guarantees: cross-process atomicity, rollback on lost races, no virtual-thread pinning, key layout with
 * TTLs, and latency.
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
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(RedissonClient.class)).thenReturn(redisson);
        ApplicationContextProvider.context = ctx;
    }

    /** 5. Two "pods" (two clients) hammering one key with 100 per window admit at most 100 in total. */
    @Test
    void crossProcessAcquiresNeverExceedLimit() throws Exception {
        String key = "rate-limiter:v2:cross:r1";
        RateLimit limits = RateLimit.builder().second(10, 100).build(); // 10 s window so the burst cannot straddle it
        LongAdder admitted = new LongAdder();
        List<Callable<Void>> work = new ArrayList<>();
        for (RedissonClient pod : List.of(redisson, secondPod))
            for (int t = 0; t < 20; t++)
                work.add(() -> {
                    for (int i = 0; i < 25; i++)
                        if (RateLimiterStore.acquire(pod, List.of(new Limited(key, limits)), System.currentTimeMillis()))
                            admitted.increment();
                    return null;
                });
        runAll(work, Executors.newFixedThreadPool(40));
        assertEquals(100, admitted.sum(), "1000 attempts from two pods must admit exactly the window limit");
        assertEquals(100L, redisson.<Long>getBucket(key + "#w:10000", LongCodec.INSTANCE).get(),
                "lost races must roll their increment back, leaving the counter at exactly the admitted count");
    }

    /** Under 200 threads through the pool API the admitted count and the stored counter both stay exactly at the limit. */
    @Test
    void concurrentAcquiresThroughPoolApiNeverExceedLimit() throws Exception {
        AdvancedRateLimiter.<String>pool("burst").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 100).maxConcurrent(1000, Duration.ofSeconds(10)).build()).build();
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
        assertEquals(100L, redisson.<Long>getBucket("rate-limiter:v2:burst:r1#w:10000", LongCodec.INSTANCE).get());
        assertEquals(100, redisson.getScoredSortedSet("rate-limiter:v2:burst:r1#p", StringCodec.INSTANCE).size(),
                "rolled-back attempts must not leak permits");
    }

    /**
     * 6. Virtual threads: 200 acquires on a virtual-thread executor record zero {@code jdk.VirtualThreadPinned}
     * events. A deliberately pinning control run first proves the recording would have caught it.
     */
    @Test
    void acquireOnVirtualThreadsDoesNotPinCarrier() throws Exception {
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

    /** Key layout: one counter per window, one sorted set of permits, one cool-off key — every one of them with a TTL. */
    @Test
    void storedKeysCarryTtlsAndMatchTheLayout() {
        AdvancedRateLimiter.<String>pool("layout").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 5).day(1000).maxConcurrent(3, Duration.ofSeconds(10)).build()).build();
        RateLimitResource<String> held = AdvancedRateLimiter.tryAcquire("layout");
        assertNotNull(held);
        assertNotNull(AdvancedRateLimiter.tryAcquire("layout"));
        held.coolOff(Duration.ofSeconds(30));

        String base = "rate-limiter:v2:layout:r1";
        assertEquals(2L, redisson.<Long>getBucket(base + "#w:10000", LongCodec.INSTANCE).get());
        assertEquals(2L, redisson.<Long>getBucket(base + "#w:86400000", LongCodec.INSTANCE).get());
        assertEquals(2, redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).size());
        assertNotNull(redisson.getBucket(base + "#c", LongCodec.INSTANCE).get());

        assertTtlWithin(base + "#w:10000", 9_000, 10_000);
        assertTtlWithin(base + "#w:86400000", 86_000_000, 86_400_000);
        assertTtlWithin(base + "#p", 86_000_000, 86_460_000); // max(window, permit timeout) + 60 s grace
        assertTtlWithin(base + "#c", 29_000, 30_000);

        held.release();
        assertEquals(1, redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).size(), "release removes one permit");
        assertEquals(2L, redisson.<Long>getBucket(base + "#w:10000", LongCodec.INSTANCE).get(), "release does not touch windows");
    }

    /** A rejected attempt writes nothing: a full window is checked, not claimed and rolled back. */
    @Test
    void rejectedAttemptLeavesStateUntouched() {
        AdvancedRateLimiter.<String>pool("reject").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 1).maxConcurrent(5, Duration.ofSeconds(10)).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("reject"));
        String base = "rate-limiter:v2:reject:r1";
        long ttlBefore = redisson.getBucket(base + "#w:10000").remainTimeToLive();

        for (int i = 0; i < 5; i++)
            assertNull(AdvancedRateLimiter.tryAcquire("reject"));

        assertEquals(1L, redisson.<Long>getBucket(base + "#w:10000", LongCodec.INSTANCE).get(), "rejects must not inflate the counter");
        assertEquals(1, redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).size(), "rejects must not add permits");
        assertTrue(redisson.getBucket(base + "#w:10000").remainTimeToLive() <= ttlBefore, "rejects must not extend the window");
    }

    /** Reject path under contention, at the key level: the storm neither inflates the counter nor extends its TTL nor leaks permits. */
    @Test
    void rejectStormLeavesKeysUntouched() throws Exception {
        AdvancedRateLimiter.<String>pool("keystorm").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 3).maxConcurrent(3, Duration.ofSeconds(10)).build()).build();
        for (int i = 0; i < 3; i++)
            assertNotNull(AdvancedRateLimiter.tryAcquire("keystorm"));
        String base = "rate-limiter:v2:keystorm:r1";
        long ttlBefore = redisson.getBucket(base + "#w:10000").remainTimeToLive();

        List<Callable<Void>> work = new ArrayList<>();
        for (int t = 0; t < 200; t++)
            work.add(() -> {
                for (int i = 0; i < 5; i++) assertNull(AdvancedRateLimiter.tryAcquire("keystorm"));
                return null;
            });
        runAll(work, Executors.newFixedThreadPool(200));

        assertEquals(3L, redisson.<Long>getBucket(base + "#w:10000", LongCodec.INSTANCE).get(), "1000 rejects must not move the counter");
        assertEquals(3, redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).size(), "rejects must not leak permits");
        long ttlAfter = redisson.getBucket(base + "#w:10000").remainTimeToLive();
        assertTrue(ttlAfter > 0 && ttlAfter <= ttlBefore, "rejects must not extend or drop the window TTL, was " + ttlBefore + " -> " + ttlAfter);
    }

    /**
     * Died mid-claim: a client that increments and adds its permit, then dies before rolling back, leaves an
     * over-count. The limiter must stay conservative (never over-admit) and heal on its own once the window and
     * the permit expire, with no manual cleanup.
     */
    @Test
    void abandonedClaimSelfHealsWithoutOverAdmitting() {
        AdvancedRateLimiter.<String>pool("died").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().millis(600, 2).maxConcurrent(2, Duration.ofMillis(400)).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("died"));
        String base = "rate-limiter:v2:died:r1";
        long now = System.currentTimeMillis();
        redisson.getAtomicLong(base + "#w:600").incrementAndGet();                                      // claim landed ...
        redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).add(now, "ghost");              // ... and the client died here

        assertNull(AdvancedRateLimiter.tryAcquire("died"), "the abandoned claim counts against the limit, never for it");
        assertFalse(AdvancedRateLimiter.canAcquire("died"));
        assertTrue(waitUntil(() -> AdvancedRateLimiter.tryAcquire("died") != null, Duration.ofSeconds(3)),
                "window and permit expiry must heal the abandoned claim");
        assertEquals(0, redisson.getScoredSortedSet(base + "#p", StringCodec.INSTANCE).count(0, true, now, true),
                "the ghost permit must have been expired out of the set");
    }

    /** Died mid-claim, worst case: a window counter that lost its TTL (INCR after expiry, then death) is re-armed by the next acquire. */
    @Test
    void windowCounterWithoutTtlIsRearmedOnNextAcquire() {
        AdvancedRateLimiter.<String>pool("stuck").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10, 100).build()).build();
        String window = "rate-limiter:v2:stuck:r1#w:10000";
        redisson.getBucket(window, LongCodec.INSTANCE).set(3L);
        assertEquals(-1, redisson.getBucket(window).remainTimeToLive(), "precondition: counter has no TTL");

        assertNotNull(AdvancedRateLimiter.tryAcquire("stuck"));

        assertEquals(4L, redisson.<Long>getBucket(window, LongCodec.INSTANCE).get());
        assertTtlWithin(window, 9_000, 10_000);
    }

    @Test
    void releaseOfLastPermitLeavesNoKeyBehind() {
        AdvancedRateLimiter.<String>pool("empty").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().maxConcurrent(1, Duration.ofSeconds(10)).build()).build();
        RateLimitResource<String> held = AdvancedRateLimiter.tryAcquire("empty");
        assertNotNull(held);
        assertTrue(redisson.getKeys().countExists("rate-limiter:v2:empty:r1#p") == 1);
        held.release();
        assertFalse(redisson.getKeys().countExists("rate-limiter:v2:empty:r1#p") == 1, "empty permit set must be gone");
    }

    @Test
    void redisFailureFallsBackToLocalLimits() {
        RedissonClient broken = mock(RedissonClient.class);
        when(broken.createBatch(any(BatchOptions.class))).thenThrow(new RuntimeException("redis down"));
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(RedissonClient.class)).thenReturn(broken);
        AdvancedRateLimiter.reset();
        ApplicationContextProvider.context = ctx;

        AdvancedRateLimiter.<String>pool("down").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(2).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("down"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("down"));
        assertNull(AdvancedRateLimiter.tryAcquire("down"), "limits must still hold on the local fallback");
    }

    /**
     * Latency against the embedded Redis; prints the numbers for the PR and guards against regressions.
     * Uncontended p99 is the gate. The 200-thread closed loop saturates the single Redis thread, so its latency
     * is queueing rather than per-call cost; it is reported as throughput.
     */
    @Test
    void acquireLatencyAndThroughput() throws Exception {
        AdvancedRateLimiter.<String>pool("perf").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(60, 10_000_000).day(10_000_000).maxConcurrent(10_000_000, Duration.ofMillis(1)).build())
                .targetLimits(TargetLimits.create().defaultLimit(RateLimit.builder().second(60, 10_000_000).build()))
                .build();
        for (int i = 0; i < 2_000; i++) // warm-up: JIT, Redisson connection pool
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

        assertTrue(p99 < 5, "uncontended p99 acquire latency regressed: " + p99 + " ms");
        assertTrue(samples.length / seconds > 500, "saturated throughput regressed: " + samples.length / seconds + " acquires/s");
    }

    // ---- helpers -------------------------------------------------------------------------------

    private static void assertTtlWithin(String key, long minMillis, long maxMillis) {
        long ttl = redisson.getBucket(key).remainTimeToLive();
        assertTrue(ttl >= minMillis && ttl <= maxMillis, key + " TTL " + ttl + " ms not within [" + minMillis + ", " + maxMillis + "]");
    }

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
