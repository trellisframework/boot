package net.trellisframework.workflow.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import net.trellisframework.data.redis.semaphore.ExpirableSemaphore;

import java.time.Duration;

@ActivityInterface
public interface DistributedLockActivity {

    int LEASE_SECONDS = 120;
    int RENEW_INTERVAL_SECONDS = 60;

    @ActivityMethod
    boolean tryAcquire(String key, String holderId, int limit);

    @ActivityMethod
    void release(String key, String holderId);

    @ActivityMethod
    void keepAlive(String key, String holderId, int limit);

    class Impl implements DistributedLockActivity {
        @Override
        public boolean tryAcquire(String key, String holderId, int limit) {
            return ExpirableSemaphore.tryAcquire(key, holderId, limit, LEASE_SECONDS);
        }

        @Override
        public void release(String key, String holderId) {
            ExpirableSemaphore.release(key, holderId);
        }

        @Override
        public void keepAlive(String key, String holderId, int limit) {
            ExpirableSemaphore.keepAlive(key, holderId, LEASE_SECONDS);
        }
    }

    static DistributedLockActivity create() {
        return Workflow.newActivityStub(
            DistributedLockActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setScheduleToStartTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setInitialInterval(Duration.ofMillis(100))
                    .setMaximumInterval(Duration.ofSeconds(2))
                    .build())
                .build()
        );
    }
}
