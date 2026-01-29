package net.trellisframework.data.redis.semaphore;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class RedisSemaphore {

    private static final String KEY_PREFIX = "semaphore:";
    private static final String LOCK_PREFIX = KEY_PREFIX + "lock:";
    private static final Map<String, Semaphore> LOCAL_SEMAPHORES = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> LOCAL_LOCKS = new ConcurrentHashMap<>();

    private static volatile RedissonClient redisson;
    private static volatile Boolean redisAvailable;

    public static boolean isAvailable() {
        if (redisAvailable != null) return redisAvailable;
        try {
            redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
            redisAvailable = redisson != null;
        } catch (Exception e) {
            redisAvailable = false;
        }
        return redisAvailable;
    }

    private static RedissonClient getRedisson() {
        if (redisson != null) return redisson;
        redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
        return redisson;
    }

    private static RSemaphore getRedisSemaphore(String key, int permits) {
        RSemaphore semaphore = getRedisson().getSemaphore(KEY_PREFIX + key);
        semaphore.trySetPermits(permits);
        return semaphore;
    }

    private static Semaphore getLocalSemaphore(String key, int permits) {
        return LOCAL_SEMAPHORES.computeIfAbsent(key + ":" + permits, k -> new Semaphore(permits, true));
    }

    public static void acquire(String key, int permits) {
        try {
            if (isAvailable()) {
                getRedisSemaphore(key, permits).acquire();
            } else {
                getLocalSemaphore(key, permits).acquire();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring semaphore", e);
        }
    }

    public static boolean tryAcquire(String key, int permits) {
        if (isAvailable()) {
            return getRedisSemaphore(key, permits).tryAcquire();
        }
        return getLocalSemaphore(key, permits).tryAcquire();
    }

    public static void release(String key, int permits) {
        if (isAvailable()) {
            getRedisSemaphore(key, permits).release();
        } else {
            getLocalSemaphore(key, permits).release();
        }
    }
}
