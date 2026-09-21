package net.trellisframework.data.redis.ratelimit;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
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
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the contract against a real Redis (embedded, started on a free port) and adds the Redis-only
 * guarantees: concurrent atomicity, no virtual-thread pinning, wire-format compatibility and latency.
 */
class AdvancedRateLimiterRedisTest extends AdvancedRateLimiterContract {

    private static RedisServer server;
    private static RedissonClient redisson;
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
        Config config = new Config();
        config.useSingleServer().setAddress(address);
        redisson = Redisson.create(config);
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisson != null) redisson.shutdown();
        if (server != null) server.stop();
    }

    @Override
    protected void wireBackend() {
        redisson.getKeys().flushdb();
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(RedissonClient.class)).thenReturn(redisson);
        ApplicationContextProvider.context = ctx;
    }

    /** 5. 40 threads hammering one key with 100 per window admit at most 100 in total. */
    @Test
    void concurrentAcquiresOnOneKeyNeverExceedLimit() throws Exception {
        String key = "rate-limiter:cross:r1";
        RateLimit limits = RateLimit.builder().second(10, 100).build(); // 10 s window so the burst cannot straddle it
        LongAdder admitted = new LongAdder();
        List<Callable<Void>> work = new ArrayList<>();
        for (int t = 0; t < 40; t++)
            work.add(() -> {
                for (int i = 0; i < 25; i++)
                    if (AdvancedRateLimiter.tryAcquireResource(key, limits))
                        admitted.increment();
                return null;
            });
        runAll(work, Executors.newFixedThreadPool(40));
        assertEquals(100, admitted.sum(), "1000 attempts must admit exactly the window limit");
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

    /** State written by a previous version is read, updated and written back in the same JSON shape and TTL. */
    @Test
    void stateJsonRoundTripsWithStoredFormat() {
        String key = "rate-limiter:compat:r1";
        RateLimit limits = RateLimit.builder().second(10, 5).maxConcurrent(3, Duration.ofSeconds(10)).build();
        long now = System.currentTimeMillis();
        ResourceState seeded = new ResourceState();
        seeded.getRates().add(ResourceState.Window.of(10_000, now, 4));
        seeded.getAcquiredTimestamps().add(now);
        seeded.setCoolOffUntil(null);
        redisson.getBucket(key, StringCodec.INSTANCE).set(JsonUtil.toString(seeded));

        assertTrue(AdvancedRateLimiter.tryAcquireResource(key, limits), "4 of 5 used → one left");
        assertFalse(AdvancedRateLimiter.tryAcquireResource(key, limits), "5 of 5 used");

        ResourceState decoded = readState(key);
        assertEquals(1, decoded.getRates().size());
        assertEquals(10_000, decoded.getRates().get(0).getDuration());
        assertEquals(now, decoded.getRates().get(0).getStartAt());
        assertEquals(5, decoded.getRates().get(0).getUsed());
        assertEquals(2, decoded.getAcquiredTimestamps().size());
        assertEquals(now, decoded.getAcquiredTimestamps().get(0));
        assertNull(decoded.getCoolOffUntil());
        long ttl = redisson.getBucket(key).remainTimeToLive();
        assertTrue(ttl > 60_000 && ttl <= 70_000, "TTL must be max window + 60 s, was " + ttl);

        long before = System.currentTimeMillis();
        AdvancedRateLimiter.applyCoolOff(key, Duration.ofMillis(500), limits);
        Long coolOffUntil = readState(key).getCoolOffUntil();
        assertNotNull(coolOffUntil);
        assertTrue(coolOffUntil >= before + 500 && coolOffUntil <= System.currentTimeMillis() + 500, "cool-off must be now + 500 ms");
    }

    @Test
    void releasingLastPermitDeletesEmptyState() {
        String key = "rate-limiter:empty:r1";
        RateLimit limits = RateLimit.builder().maxConcurrent(1, Duration.ofSeconds(10)).build();
        assertTrue(AdvancedRateLimiter.tryAcquireResource(key, limits));
        assertTrue(redisson.getBucket(key).isExists());
        AdvancedRateLimiter.releaseResource(key, limits);
        assertFalse(redisson.getBucket(key).isExists(), "empty state must be deleted, not stored");
    }

    @Test
    void survivesScriptCacheFlush() {
        AdvancedRateLimiter.<String>pool("flush").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(2).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("flush"));
        redisson.getScript().scriptFlush(); // e.g. Redis restarted → NOSCRIPT → Redisson reloads its scripts transparently
        assertNotNull(AdvancedRateLimiter.tryAcquire("flush"));
        assertNull(AdvancedRateLimiter.tryAcquire("flush"), "state must have survived the reload");
    }

    @Test
    void redisFailureFallsBackToLocalLimits() {
        RedissonClient broken = mock(RedissonClient.class, invocation -> {
            if (invocation.getMethod().getDeclaringClass() == Object.class)
                return RETURNS_DEFAULTS.answer(invocation);
            throw new RuntimeException("redis down");
        });
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
     * Latency against the embedded Redis; prints the numbers for the PR and guards against regressions. Limits
     * mirror production (fixed windows plus a small, expiring permit set).
     * <p>
     * Uncontended p99 is the gate. The 200-thread closed loop serializes on one key, so its latency
     * is queueing (in-flight / throughput) rather than per-call cost; it is reported as throughput.
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

    private static ResourceState readState(String key) {
        return JsonUtil.toObject(redisson.<String>getBucket(key, StringCodec.INSTANCE).get(), ResourceState.class);
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
