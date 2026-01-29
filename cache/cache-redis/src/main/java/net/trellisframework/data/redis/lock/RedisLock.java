package net.trellisframework.data.redis.lock;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class RedisLock {

    private static final String KEY_PREFIX = "distributed-lock:";
    private static final Map<String, Semaphore> LOCAL_SEMAPHORES = new ConcurrentHashMap<>();

    private static volatile RedissonClient redisson;
    private static volatile Boolean redisAvailable;

    private static RedissonClient getRedisson() {
        if (redisson != null) return redisson;
        redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
        return redisson;
    }

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

    public static boolean tryLock(String key, String lockId, int limit, int ttlSeconds) {
        if (isAvailable()) {
            RMap<String, Long> holders = getRedisson().getMap(KEY_PREFIX + key);
            long now = Instant.now().getEpochSecond();
            long expireThreshold = now - ttlSeconds;
            holders.entrySet().removeIf(e -> e.getValue() < expireThreshold);
            if (holders.size() < limit) {
                holders.put(lockId, now);
                return true;
            }
            return false;
        }
        return LOCAL_SEMAPHORES.computeIfAbsent(key + ":" + limit, k -> new Semaphore(limit)).tryAcquire();
    }

    public static boolean keepAlive(String key, String lockId) {
        if (isAvailable()) {
            RMap<String, Long> holders = getRedisson().getMap(KEY_PREFIX + key);
            if (holders.containsKey(lockId)) {
                holders.put(lockId, Instant.now().getEpochSecond());
                return true;
            }
            return false;
        }
        return true;
    }

    public static boolean unlock(String key, String lockId) {
        try {
            if (isAvailable())
                return getRedisson().getMap(KEY_PREFIX + key).remove(lockId) != null;
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
