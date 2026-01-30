package net.trellisframework.data.redis.semaphore;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExpirableSemaphore {

    private static final String KEY_PREFIX = "semaphore:";
    private static final String PERMIT_PREFIX = "permit:";
    private static final Map<String, Semaphore> LOCAL = new ConcurrentHashMap<>();

    private static volatile RedissonClient redisson;
    private static volatile Boolean available;

    private static RedissonClient redis() {
        if (redisson == null)
            redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
        return redisson;
    }

    public static boolean isAvailable() {
        if (available == null) {
            try {
                available = redis() != null;
            } catch (Exception e) {
                available = false;
            }
        }
        return available;
    }

    public static boolean tryAcquire(String key, String holderId, int limit, int leaseSeconds) {
        if (isAvailable()) {
            try {
                RPermitExpirableSemaphore sem = redis().getPermitExpirableSemaphore(KEY_PREFIX + key);
                sem.trySetPermits(limit);
                String permitId = sem.tryAcquire(0, leaseSeconds, TimeUnit.SECONDS);
                if (permitId != null) {
                    redis().getBucket(PERMIT_PREFIX + holderId).set(permitId, java.time.Duration.ofSeconds(leaseSeconds));
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
        return LOCAL.computeIfAbsent(key, k -> new Semaphore(limit)).tryAcquire();
    }

    public static void keepAlive(String key, String holderId, int leaseSeconds) {
        try {
            if (isAvailable()) {
                String permitId = (String) redis().getBucket(PERMIT_PREFIX + holderId).get();
                if (permitId != null) {
                    redis().getPermitExpirableSemaphore(KEY_PREFIX + key).updateLeaseTime(permitId, leaseSeconds, TimeUnit.SECONDS);
                    redis().getBucket(PERMIT_PREFIX + holderId).expire(java.time.Duration.ofSeconds(leaseSeconds));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void release(String key, String holderId) {
        try {
            if (isAvailable()) {
                String permitId = (String) redis().getBucket(PERMIT_PREFIX + holderId).getAndDelete();
                Optional.ofNullable(permitId).ifPresent(x -> redis().getPermitExpirableSemaphore(KEY_PREFIX + key).release(x));
            } else {
                Optional.ofNullable(LOCAL.get(key)).ifPresent(Semaphore::release);
            }
        } catch (Exception ignored) {
        }
    }
}
