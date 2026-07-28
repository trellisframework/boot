package net.trellisframework.workflow.temporal.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.redis.semaphore.ExpirableSemaphore;
import net.trellisframework.workflow.temporal.workflow.ConcurrencyTickerWorkflow;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ActivityInterface
public interface DispatcherStore {

    int LEASE_SECONDS = DistributedLockActivity.LEASE_SECONDS;

    String QUEUE_KEY_PREFIX = "dispatcher:queue:";

    @ActivityMethod
    List<Claim> claim(int maxBatch);

    class Claim {
        public String key;
        public String holderId;
        public int limit;
        public List<Object> workArgs;

        public Claim() {
        }

        public Claim(String key, String holderId, int limit, List<Object> workArgs) {
            this.key = key;
            this.holderId = holderId;
            this.limit = limit;
            this.workArgs = workArgs;
        }
    }

    class Impl implements DispatcherStore {
        private static final String QUEUE = QUEUE_KEY_PREFIX;
        private static final String LIMIT = "dispatcher:limit:";
        private static final String KEYS = "dispatcher:keys";
        private final ObjectMapper mapper = new ObjectMapper();

        private RedissonClient redis() {
            return ApplicationContextProvider.context.getBean(RedissonClient.class);
        }

        public void enqueue(String key, int limit, String taskQueue, List<Object> workArgs) {
            RedissonClient r = redis();
            try {
                r.getBucket(LIMIT + key).set(limit);
                r.getDeque(QUEUE + key).addLast(mapper.writeValueAsString(workArgs));
                r.getSet(KEYS).add(key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to enqueue concurrency-gated work for key " + key, e);
            }
            ensureTickerRunning(taskQueue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Claim> claim(int maxBatch) {
            RedissonClient r = redis();
            List<Claim> claims = new ArrayList<>();
            for (Object keyObj : r.getSet(KEYS).readAll()) {
                if (claims.size() >= maxBatch) break;
                String key = (String) keyObj;
                var queue = r.getDeque(QUEUE + key);
                int limit = asInt(r.getBucket(LIMIT + key).get(), 1);
                if (queue.isEmpty()) {
                    removeKeyIfEmpty(r, key);
                    continue;
                }
                while (claims.size() < maxBatch) {
                    if (queue.isEmpty()) {
                        removeKeyIfEmpty(r, key);
                        break;
                    }
                    String holderId = key + ":" + UUID.randomUUID();
                    if (!ExpirableSemaphore.tryAcquire(key, holderId, limit, LEASE_SECONDS))
                        break;
                    Object raw = queue.pollFirst();
                    if (raw == null) {
                        ExpirableSemaphore.release(key, holderId);
                        break;
                    }
                    try {
                        List<Object> workArgs = mapper.readValue((String) raw, List.class);
                        claims.add(new Claim(key, holderId, limit, workArgs));
                    } catch (Exception e) {
                        ExpirableSemaphore.release(key, holderId);
                    }
                }
            }
            return claims;
        }

        private void removeKeyIfEmpty(RedissonClient r, String key) {
            r.getSet(KEYS).remove(key);
            if (!r.getDeque(QUEUE + key).isEmpty())
                r.getSet(KEYS).add(key);
        }

        private int asInt(Object value, int fallback) {
            return value instanceof Number n ? n.intValue() : fallback;
        }

        private void ensureTickerRunning(String taskQueue) {
            WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(ConcurrencyTickerWorkflow.WORKFLOW_ID)
                    .setTaskQueue(taskQueue)
                    .setWorkflowTaskTimeout(Duration.ofSeconds(120))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build();
            WorkflowStub stub = client.newUntypedWorkflowStub("DynamicWorkflowAction", options);

            stub.signalWithStart("wake", new Object[]{}, new Object[]{ConcurrencyTickerWorkflow.CLASS_NAME});
        }
    }

    static int pendingCount(String key) {
        try {
            return ApplicationContextProvider.context.getBean(RedissonClient.class).getDeque(QUEUE_KEY_PREFIX + key).size();
        } catch (Exception e) {
            return 0;
        }
    }

    static DispatcherStore create() {
        return Workflow.newActivityStub(
            DispatcherStore.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToStartTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(10)
                    .setInitialInterval(Duration.ofMillis(200))
                    .setMaximumInterval(Duration.ofSeconds(5))
                    .build())
                .build()
        );
    }
}
