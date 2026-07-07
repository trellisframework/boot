package net.trellisframework.workflow.temporal.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import net.trellisframework.data.redis.semaphore.ExpirableSemaphore;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.Workflow;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.workflow.temporal.action.WorkflowAction1;
import net.trellisframework.workflow.temporal.activity.DispatcherActivity;
import net.trellisframework.workflow.temporal.activity.DispatcherStore;
import net.trellisframework.workflow.temporal.activity.DistributedLockActivity;
import net.trellisframework.workflow.temporal.activity.DynamicTaskActivity;
import net.trellisframework.workflow.temporal.config.WorkflowProperties;
import net.trellisframework.workflow.temporal.config.WorkflowProperties.ConcurrencyBackend;
import net.trellisframework.workflow.temporal.util.ConcurrencyArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P0 harness for the REDIS concurrency backend (durable Redis queue drained by the global
 * {@link ConcurrencyTickerWorkflow}). Drives the real backend end-to-end in a {@link TestWorkflowEnvironment}:
 * enqueue → ticker claim (leased permit) → child ({@link RecorderWorkflow}) → release.
 *
 * <p>Wiring notes:
 * <ul>
 *   <li><b>Real Redis</b> on {@code 127.0.0.1:6379} DB 15 (flushed per test); the ticker/store/semaphore
 *       are not mockable (durability + atomic permits are the point). Skips if Redis is unreachable.</li>
 *   <li><b>{@code setUseTimeskipping(false)}</b> is mandatory — time-skipping collapses the ticker's 10s poll
 *       and 5-min idle exit, causing false drops. Consequence: tests run in real wall-clock (tens of seconds).</li>
 *   <li>Lives in this package so T-2 can lower {@link ConcurrencyTickerWorkflow#MAX_HISTORY} to force a real
 *       ContinueAsNew.</li>
 * </ul>
 * Maps to {@code boot-concurrency-backend-test-plan.md} (T-1…T-6).
 */
class RedisConcurrencyBackendTest {

    private static final String TASK_QUEUE = "concurrency-backend-test";
    private static final long WORK_MILLIS = 800; // per-child body time — long enough to overlap, short vs the 10s tick

    private static RedissonClient redisson;
    private TestWorkflowEnvironment env;
    private WorkflowProperties props;

    // ---- lifecycle -----------------------------------------------------------------------------

    @BeforeAll
    static void connectRedis() {
        assumeTrue(reachable("127.0.0.1", 6379), "Redis not reachable on 127.0.0.1:6379 — skipping REDIS backend tests");
        Config cfg = new Config();
        cfg.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(15);
        redisson = Redisson.create(cfg);
    }

    @AfterAll
    static void closeRedis() {
        if (redisson != null) redisson.shutdown();
    }

    @BeforeEach
    void setUp() {
        redisson.getKeys().flushdb(); // isolate: clear DB 15 (queues, semaphores, permits) before each test
        Recorder.reset();

        env = TestWorkflowEnvironment.newInstance(TestEnvironmentOptions.newBuilder().setUseTimeskipping(false).build());
        Worker worker = env.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(DynamicWorkflowAction.class);
        worker.registerActivitiesImplementations(
                new DynamicTaskActivity(),
                new DistributedLockActivity.Impl(),
                new DispatcherActivity.Impl(),
                new DispatcherStore.Impl(),
                new RecorderActivity.Impl());
        env.start();

        props = new WorkflowProperties();
        props.setConcurrencyBackend(ConcurrencyBackend.REDIS);

        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(WorkflowClient.class)).thenReturn(env.getWorkflowClient());
        when(ctx.getBean(RedissonClient.class)).thenReturn(redisson);
        when(ctx.getBean(WorkflowProperties.class)).thenReturn(props);
        when(ctx.getBean(RecorderWorkflow.class)).thenReturn(new RecorderWorkflow());
        ApplicationContextProvider.context = ctx;
    }

    @AfterEach
    void tearDown() {
        ConcurrencyTickerWorkflow.MAX_HISTORY = 5000;          // undo any T-2 override
        ConcurrencyTickerWorkflow.IDLE_TICKS_BEFORE_EXIT = 30; // undo any T-9 override
        if (env != null) env.close();
    }

    // ---- smoke ---------------------------------------------------------------------------------

    @Test
    void smoke_singleItemRunsThroughRedisBackend() {
        enqueue("gate-smoke", 1, "item-1", null);

        assertTrue(waitUntil(() -> Recorder.started.contains("item-1"), Duration.ofSeconds(30)),
                "item never ran through the REDIS backend");
        assertEquals(1, Recorder.startCount.getOrDefault("item-1", 0), "item ran more than once");
        assertTrue(waitUntil(() -> DispatcherStore.pendingCount("gate-smoke") == 0, Duration.ofSeconds(5)),
                "queue not drained");
    }

    // ---- P0 cases (see boot-concurrency-backend-test-plan.md) -----------------------------------

    /** T-1 · No loss under a burst: every enqueued item runs exactly once, queue fully drains. */
    @Test
    void t1_noLossUnderBurst() {
        int n = 6, limit = 3;
        for (int i = 0; i < n; i++) enqueue("gate-t1", limit, "t1-" + i, null);

        assertTrue(waitUntil(() -> Recorder.started.size() >= n, Duration.ofSeconds(60)),
                "not all items ran: saw " + Recorder.started.size() + "/" + n);
        for (int i = 0; i < n; i++)
            assertEquals(1, Recorder.startCount.getOrDefault("t1-" + i, 0), "t1-" + i + " ran wrong number of times");
        assertTrue(waitUntil(() -> DispatcherStore.pendingCount("gate-t1") == 0, Duration.ofSeconds(5)), "queue not drained");
    }

    /**
     * T-2 · ContinueAsNew carries no state: with MAX_HISTORY lowered the ticker CANs repeatedly mid-drain;
     * because the queue lives in Redis (not workflow state), nothing is lost or duplicated across the CANs.
     */
    @Test
    void t2_tickerContinueAsNewCarriesNoState() {
        ConcurrencyTickerWorkflow.MAX_HISTORY = 25; // force frequent CANs during the drain (restored in tearDown)
        int n = 9, limit = 3;
        for (int i = 0; i < n; i++) enqueue("gate-t2", limit, "t2-" + i, null);

        assertTrue(waitUntil(() -> Recorder.started.size() >= n, Duration.ofSeconds(90)),
                "not all items survived ContinueAsNew: saw " + Recorder.started.size() + "/" + n);
        for (int i = 0; i < n; i++)
            assertEquals(1, Recorder.startCount.getOrDefault("t2-" + i, 0), "t2-" + i + " lost or duplicated across CAN");
        assertTrue(waitUntil(() -> DispatcherStore.pendingCount("gate-t2") == 0, Duration.ofSeconds(5)), "queue not drained");
    }

    /**
     * T-3 · Durability across ticker death: kill the ticker while work is still queued in Redis; the waiting
     * items survive (not lost), and a later enqueue revives the ticker (SignalWithStart) and drains everything.
     */
    @Test
    void t3_durabilityAcrossTickerDeath() {
        int n = 4, limit = 2;
        for (int i = 0; i < n; i++) enqueue("gate-t3", limit, "t3-" + i, null);

        // wait until the first claim batch (== limit) has fully started, so nothing is mid-claim when we kill it
        assertTrue(waitUntil(() -> Recorder.started.size() >= limit, Duration.ofSeconds(30)), "ticker never started work");

        env.getWorkflowClient().newUntypedWorkflowStub(ConcurrencyTickerWorkflow.WORKFLOW_ID)
                .terminate("t3: simulate ticker death");

        // the not-yet-claimed items must still be sitting durably in Redis
        assertTrue(waitUntil(() -> DispatcherStore.pendingCount("gate-t3") >= n - limit, Duration.ofSeconds(5)),
                "waiting work was lost when the ticker died");

        // revive via a fresh enqueue → SignalWithStart restarts the ticker; everything must drain, nothing lost
        enqueue("gate-t3", limit, "t3-revive", null);
        assertTrue(waitUntil(() -> Recorder.started.size() >= n + 1, Duration.ofSeconds(60)),
                "work did not resume after ticker revival: saw " + Recorder.started.size() + "/" + (n + 1));
        for (int i = 0; i < n; i++)
            assertEquals(1, Recorder.startCount.getOrDefault("t3-" + i, 0), "t3-" + i + " lost or duplicated across ticker death");
        assertEquals(1, Recorder.startCount.getOrDefault("t3-revive", 0), "revive item ran wrong number of times");
    }

    /** T-4 · Exactly-once child start: two enqueues with the same idempotency key start one child. */
    @Test
    void t4_exactlyOnceOnDuplicateIdempotencyKey() {
        enqueue("gate-t4", 5, "t4-dup", "idem-key-t4");
        enqueue("gate-t4", 5, "t4-dup", "idem-key-t4");

        assertTrue(waitUntil(() -> Recorder.started.contains("t4-dup"), Duration.ofSeconds(30)), "item never ran");
        waitUntil(() -> false, Duration.ofSeconds(3)); // give any duplicate a chance to (wrongly) start
        assertEquals(1, Recorder.startCount.getOrDefault("t4-dup", 0), "duplicate idempotency key started the child twice");
    }

    /** T-5 · Per-key limit strictly enforced: peak concurrency never exceeds the limit. */
    @Test
    void t5_perKeyLimitEnforced() {
        int n = 6, limit = 3;
        for (int i = 0; i < n; i++) enqueue("gate-t5", limit, "t5-" + i, null);

        assertTrue(waitUntil(() -> Recorder.started.size() >= n, Duration.ofSeconds(60)), "not all items ran");
        assertTrue(Recorder.maxConcurrent.get() <= limit,
                "peak concurrency " + Recorder.maxConcurrent.get() + " exceeded limit " + limit);
    }

    /**
     * T-6 · Backend routing: the same enqueue call routes by {@code workflow.concurrency-backend}. REDIS pushes
     * to the Redis queue (proved with limit 0 so it can never be claimed); MEMORY uses the in-memory dispatcher
     * and never touches Redis.
     */
    @Test
    void t6_configSwitchRouting() {
        // REDIS → item lands in Redis; limit 0 means it can never be claimed, so it stays put and nothing runs
        props.setConcurrencyBackend(ConcurrencyBackend.REDIS);
        enqueue("gate-t6-redis", 0, "t6-redis", null);
        assertTrue(waitUntil(() -> DispatcherStore.pendingCount("gate-t6-redis") == 1, Duration.ofSeconds(10)),
                "REDIS backend did not enqueue into Redis");
        assertEquals(0, Recorder.startCount.getOrDefault("t6-redis", 0), "limit-0 REDIS item should not have run");

        // MEMORY → in-memory dispatcher runs it, Redis untouched
        props.setConcurrencyBackend(ConcurrencyBackend.MEMORY);
        enqueue("gate-t6-mem", 1, "t6-mem", null);
        assertTrue(waitUntil(() -> Recorder.started.contains("t6-mem"), Duration.ofSeconds(30)),
                "MEMORY backend did not run the item via the in-memory dispatcher");
        assertEquals(0, DispatcherStore.pendingCount("gate-t6-mem"), "MEMORY backend wrongly wrote to Redis");
    }

    // ---- P1 cases ------------------------------------------------------------------------------

    /**
     * T-7 · Leaked-slot self-heal: a holder that dies without releasing keeps its permit only until the lease
     * expires, after which the slot is reclaimed. Tested at the {@link ExpirableSemaphore} level with a short
     * lease (the ticker wires this same mechanism at LEASE_SECONDS=120).
     */
    @Test
    void t7_leakedPermitSelfHeals() {
        String key = "gate-t7";
        int limit = 2, leaseSeconds = 2;
        assertTrue(ExpirableSemaphore.tryAcquire(key, key + ":h1", limit, leaseSeconds), "h1 should acquire");
        assertTrue(ExpirableSemaphore.tryAcquire(key, key + ":h2", limit, leaseSeconds), "h2 should acquire");
        // gate full — a third acquire is refused
        assertFalse(ExpirableSemaphore.tryAcquire(key, key + ":h3", limit, leaseSeconds), "gate should be full");
        // h1/h2 "died" (never renewed, never released) → their leases lapse → a slot frees up
        assertTrue(waitUntil(() -> ExpirableSemaphore.tryAcquire(key, key + ":h4", limit, leaseSeconds), Duration.ofSeconds(6)),
                "leaked permit did not self-heal after lease expiry");
    }

    /**
     * T-8 · Permit renewal keeps a long-held slot alive: renewing before expiry extends the lease, so the slot
     * stays held past its original lease and no second holder can take it; release then frees it. (Ticker renews
     * every RENEW_INTERVAL_SECONDS=60 against a LEASE_SECONDS=120 lease.)
     */
    @Test
    void t8_renewalKeepsPermitAlive() throws InterruptedException {
        String key = "gate-t8", holder = key + ":h1";
        int limit = 1, leaseSeconds = 2;
        assertTrue(ExpirableSemaphore.tryAcquire(key, holder, limit, leaseSeconds), "h1 should acquire");
        Thread.sleep(1000);
        ExpirableSemaphore.keepAlive(key, holder, leaseSeconds); // renew at t=1s → new expiry t=3s
        Thread.sleep(1400);                                      // t=2.4s: expired WITHOUT renewal, still held WITH it
        assertFalse(ExpirableSemaphore.tryAcquire(key, key + ":h2", limit, leaseSeconds), "permit expired despite renewal");
        ExpirableSemaphore.release(key, holder);
        assertTrue(ExpirableSemaphore.tryAcquire(key, key + ":h3", limit, leaseSeconds), "slot not freed after release");
    }

    /**
     * T-9 · Idle exit then revival: with the idle threshold lowered, the drained ticker exits; a later enqueue
     * revives it via SignalWithStart and the new item runs.
     */
    @Test
    void t9_idleExitThenRevival() {
        ConcurrencyTickerWorkflow.IDLE_TICKS_BEFORE_EXIT = 2; // ~20s idle → exit (restored in tearDown)
        enqueue("gate-t9", 1, "t9-a", null);
        assertTrue(waitUntil(() -> Recorder.started.contains("t9-a"), Duration.ofSeconds(30)), "first item never ran");
        // idle well past the ~20s exit threshold so the ticker has certainly closed before we revive it
        waitUntil(() -> false, Duration.ofSeconds(30));
        enqueue("gate-t9", 1, "t9-b", null);
        assertTrue(waitUntil(() -> Recorder.started.contains("t9-b"), Duration.ofSeconds(30)),
                "ticker was not revived after idle exit");
    }

    /**
     * T-10 · Multi-key independence: one saturated key with a backlog must not starve another key. The single
     * global ticker serves both; key B runs promptly despite key A's backlog, and everything drains.
     */
    @Test
    void t10_multiKeyIndependence() {
        enqueue("gate-A", 1, "A-0", null);
        enqueue("gate-A", 1, "A-1", null);
        enqueue("gate-A", 1, "A-2", null);
        enqueue("gate-B", 1, "B-0", null);

        assertTrue(waitUntil(() -> Recorder.started.contains("B-0"), Duration.ofSeconds(30)),
                "key B was starved by key A's backlog");
        assertTrue(waitUntil(() -> Recorder.started.size() >= 4, Duration.ofSeconds(60)), "not all items across keys ran");
        assertEquals(1, Recorder.startCount.getOrDefault("B-0", 0), "B-0 ran wrong number of times");
    }

    /**
     * T-11 · Arg framing keys on position, not value: a business arg equal to the ticker's SLOT_MARKER must not
     * be mistaken for the appended slot metadata — the workflow still receives its real arg and runs.
     */
    @Test
    void t11_argFramingDisambiguation() {
        String trickyId = ConcurrencyTickerWorkflow.SLOT_MARKER; // business arg that looks like the marker
        enqueue("gate-t11", 1, trickyId, null);
        assertTrue(waitUntil(() -> Recorder.started.contains(trickyId), Duration.ofSeconds(30)),
                "arg framing mis-parsed a business value that looks like the slot marker");
        assertEquals(1, Recorder.startCount.getOrDefault(trickyId, 0), "item ran wrong number of times");
    }

    /**
     * T-12 · workArgs survive the Redis round-trip: {@code claim} deserializes exactly what {@code enqueue}
     * serialized, including a non-String (Map) arg. Seeds Redis directly (no ticker) and claims in-process.
     */
    @Test
    void t12_workArgsRoundTripThroughRedisQueue() throws Exception {
        List<Object> original = List.of(RecorderWorkflow.class.getName(), "track-xyz", Map.of("token", "T", "seat", 3));
        // mimic enqueue's Redis writes WITHOUT ensureTickerRunning, so no ticker races our direct claim
        redisson.getBucket("dispatcher:limit:gate-t12").set(5);
        redisson.getDeque("dispatcher:queue:gate-t12").addLast(new ObjectMapper().writeValueAsString(original));
        redisson.getSet("dispatcher:keys").add("gate-t12");

        List<DispatcherStore.Claim> claims = new DispatcherStore.Impl().claim(10);

        assertEquals(1, claims.size(), "expected exactly one claim");
        assertEquals(original, claims.get(0).workArgs, "workArgs did not survive the Redis JSON round-trip");
    }

    // ---- P2 cases ------------------------------------------------------------------------------

    /**
     * T-15 · Redis-down surfaces a failure, not silent loss: when the Redis client throws, {@code enqueue}
     * propagates a failure (so in prod the enclosing Temporal activity retries) rather than swallowing the
     * work item. Nothing runs and nothing is silently accepted.
     */
    @Test
    void t15_redisDownSurfacesFailureNotSilentLoss() {
        RedissonClient broken = mock(RedissonClient.class);
        when(broken.getBucket(anyString())).thenThrow(new RuntimeException("redis down"));

        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(RedissonClient.class)).thenReturn(broken);
        when(ctx.getBean(WorkflowClient.class)).thenReturn(env.getWorkflowClient());
        when(ctx.getBean(WorkflowProperties.class)).thenReturn(props);
        ApplicationContextProvider.context = ctx;

        List<Object> workArgs = new ArrayList<>(List.of(RecorderWorkflow.class.getName(), "t15"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new DispatcherStore.Impl().enqueue("gate-t15", 1, TASK_QUEUE, workArgs),
                "enqueue against a down Redis must throw, not swallow the work");
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("Failed to enqueue"),
                "failure should be a clear enqueue error, was: " + ex.getMessage());
        assertFalse(Recorder.started.contains("t15"), "no work should have run under a broken Redis");
    }

    // ---- helpers -------------------------------------------------------------------------------

    /** Enqueue one concurrency-gated {@link RecorderWorkflow} run via the real backend router. */
    private void enqueue(String concurrencyKey, int limit, String id, String idempotencyKey) {
        List<Object> workArgs = ConcurrencyArgs.withKey(
                new ArrayList<>(List.of(RecorderWorkflow.class.getName(), id)), idempotencyKey);
        new DispatcherActivity.Impl().enqueue(concurrencyKey, limit, 50, TASK_QUEUE, workArgs);
    }

    private static boolean waitUntil(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        }
        return condition.getAsBoolean();
    }

    private static boolean reachable(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- test workflow + recorder --------------------------------------------------------------

    /** Minimal concurrency-gated business workflow: mark start (via activity), hold a slot, mark end. */
    public static class RecorderWorkflow implements WorkflowAction1<Void, String> {
        @Override
        public Void execute(String id) {
            RecorderActivity rec = Workflow.newActivityStub(RecorderActivity.class,
                    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());
            rec.enter(id);
            Workflow.sleep(Duration.ofMillis(WORK_MILLIS));
            rec.exit(id);
            return null;
        }
    }

    @ActivityInterface
    public interface RecorderActivity {
        @ActivityMethod void enter(String id);
        @ActivityMethod void exit(String id);

        class Impl implements RecorderActivity {
            @Override public void enter(String id) { Recorder.enter(id); }
            @Override public void exit(String id) { Recorder.exit(id); }
        }
    }

    /** In-JVM observation of what actually ran (activities run in-process under TestWorkflowEnvironment). */
    static final class Recorder {
        static final Set<String> started = ConcurrentHashMap.newKeySet();
        static final Map<String, Integer> startCount = new ConcurrentHashMap<>();
        static final AtomicInteger running = new AtomicInteger();
        static final AtomicInteger maxConcurrent = new AtomicInteger();

        static void enter(String id) {
            started.add(id);
            startCount.merge(id, 1, Integer::sum);
            int r = running.incrementAndGet();
            maxConcurrent.accumulateAndGet(r, Math::max);
        }

        static void exit(String id) {
            running.decrementAndGet();
        }

        static void reset() {
            started.clear();
            startCount.clear();
            running.set(0);
            maxConcurrent.set(0);
        }
    }
}
