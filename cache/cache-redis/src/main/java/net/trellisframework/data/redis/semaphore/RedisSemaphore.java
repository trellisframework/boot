package net.trellisframework.data.redis.semaphore;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Distributed semaphore with Redis (Redisson) and local fallback.
 * <p>
 * If Redis is available, uses distributed semaphore across all JVM instances.
 * If Redis is not available, falls back to local JVM semaphore (single instance only).
 * <p>
 * Example:
 * <pre>
 * RedisSemaphore.acquire("customer-123", 5);
 * try {
 *     // do work (max 5 concurrent)
 * } finally {
 *     RedisSemaphore.release("customer-123", 5);
 * }
 * </pre>
 */
public class RedisSemaphore {

    private static final String KEY_PREFIX = "semaphore:";

    // Local fallback when Redis is not available
    private static final Map<String, Semaphore> LOCAL_SEMAPHORES = new ConcurrentHashMap<>();

    private static volatile RedissonClient redisson;
    private static volatile Boolean redisAvailable;

    /**
     * Check if Redis is available (cached result).
     */
    public static boolean isAvailable() {
        if (redisAvailable != null) {
            return redisAvailable;
        }
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
        String semaphoreKey = key + ":" + permits;
        return LOCAL_SEMAPHORES.computeIfAbsent(semaphoreKey, k -> new Semaphore(permits, true));
    }

    /**
     * Acquire a permit, blocking until one is available.
     * Uses Redis if available, otherwise falls back to local semaphore.
     *
     * @param key the semaphore key
     * @param permits the total number of permits (concurrency limit)
     */
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

    /**
     * Try to acquire a permit within the given timeout.
     *
     * @param key the semaphore key
     * @param permits the total number of permits (concurrency limit)
     * @param timeout maximum time to wait
     * @return true if permit was acquired, false if timeout
     */
    public static boolean tryAcquire(String key, int permits, Duration timeout) {
        try {
            if (isAvailable()) {
                return getRedisSemaphore(key, permits).tryAcquire(timeout);
            } else {
                return getLocalSemaphore(key, permits).tryAcquire(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Try to acquire a permit without waiting.
     *
     * @param key the semaphore key
     * @param permits the total number of permits (concurrency limit)
     * @return true if permit was acquired immediately, false otherwise
     */
    public static boolean tryAcquire(String key, int permits) {
        if (isAvailable()) {
            return getRedisSemaphore(key, permits).tryAcquire();
        } else {
            return getLocalSemaphore(key, permits).tryAcquire();
        }
    }

    /**
     * Release a permit.
     *
     * @param key the semaphore key
     * @param permits the total number of permits (must match acquire call)
     */
    public static void release(String key, int permits) {
        if (isAvailable()) {
            getRedisSemaphore(key, permits).release();
        } else {
            getLocalSemaphore(key, permits).release();
        }
    }

    /**
     * Get the number of available permits.
     *
     * @param key the semaphore key
     * @param permits the total number of permits
     * @return number of available permits
     */
    public static int availablePermits(String key, int permits) {
        if (isAvailable()) {
            return getRedisSemaphore(key, permits).availablePermits();
        } else {
            return getLocalSemaphore(key, permits).availablePermits();
        }
    }
}
