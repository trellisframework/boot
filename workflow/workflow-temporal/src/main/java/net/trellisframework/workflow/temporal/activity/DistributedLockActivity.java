package net.trellisframework.workflow.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.workflow.Workflow;
import net.trellisframework.data.redis.lock.RedisLock;

import java.time.Duration;

@ActivityInterface
public interface DistributedLockActivity {

    int TTL_SECONDS = 5;

    @ActivityMethod
    boolean tryLock(String key, String lockId, int limit);

    @ActivityMethod
    boolean keepAlive(String key, String lockId);

    class Impl implements DistributedLockActivity {
        @Override
        public boolean tryLock(String key, String lockId, int limit) {
            return RedisLock.tryLock(key, lockId, limit, TTL_SECONDS);
        }

        @Override
        public boolean keepAlive(String key, String lockId) {
            return RedisLock.keepAlive(key, lockId);
        }
    }

    static DistributedLockActivity create() {
        return Workflow.newLocalActivityStub(
            DistributedLockActivity.class,
            LocalActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(5))
                .build()
        );
    }
}
