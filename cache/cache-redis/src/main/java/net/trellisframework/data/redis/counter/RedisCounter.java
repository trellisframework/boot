package net.trellisframework.data.redis.counter;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RAtomicDouble;
import org.redisson.api.RedissonClient;

import java.time.Duration;

public class RedisCounter {

    private static RedissonClient redisson;

    private static RedissonClient redis() {
        if (redisson != null)
            return redisson;
        return redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
    }

    private static RAtomicDouble getCounter(String key) {
        return redis().getAtomicDouble(key);
    }

    public static long increment(String key) {
        return (long) getCounter(key).incrementAndGet();
    }

    public static long increment(String key, long delta) {
        return (long) getCounter(key).addAndGet(delta);
    }

    public static double increment(String key, double delta) {
        return getCounter(key).addAndGet(delta);
    }

    public static long increment(String key, Duration ttl) {
        return increment(key, 1L, ttl);
    }

    public static long increment(String key, long delta, Duration ttl) {
        return (long) increment(key, (double) delta, ttl);
    }

    public static double increment(String key, double delta, Duration ttl) {
        RAtomicDouble counter = getCounter(key);
        double result = counter.addAndGet(delta);
        if (counter.remainTimeToLive() == -1) {
            counter.expire(ttl);
        }
        return result;
    }

    public static long decrement(String key) {
        return (long) getCounter(key).decrementAndGet();
    }

    public static long decrement(String key, long delta) {
        return (long) getCounter(key).addAndGet(-delta);
    }

    public static double decrement(String key, double delta) {
        return getCounter(key).addAndGet(-delta);
    }

    public static long decrement(String key, Duration ttl) {
        return increment(key, -1L, ttl);
    }

    public static long decrement(String key, long delta, Duration ttl) {
        return increment(key, -delta, ttl);
    }

    public static double decrement(String key, double delta, Duration ttl) {
        return increment(key, -delta, ttl);
    }

    public static long getLong(String key) {
        return (long) getCounter(key).get();
    }

    public static double getDouble(String key) {
        return getCounter(key).get();
    }

    public static boolean exists(String key) {
        return getCounter(key).isExists();
    }

    public static void delete(String key) {
        getCounter(key).delete();
    }

    public static long ttl(String key) {
        return getCounter(key).remainTimeToLive();
    }
}
