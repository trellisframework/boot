
package net.trellisframework.data.redis.counter;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class RedisSlidingWindowCounter {

    private static final String KEY_PREFIX = "swc:";
    private static final Duration DEFAULT_RETENTION = Duration.ofHours(24);
    private static RedissonClient redisson;

    public static void record(String key) {
        record(key, Instant.now(), DEFAULT_RETENTION);
    }

    public static void record(String key, Duration retention) {
        record(key, Instant.now(), retention);
    }

    public static void record(String key, Instant timestamp, Duration retention) {
        var set = getSet(key);
        if (set == null) return;
        set.add(timestamp.toEpochMilli(), UUID.randomUUID().toString());
        set.removeRangeByScore(0, true, Instant.now().minus(retention).toEpochMilli(), true);
        set.expire(retention.plusMinutes(1));
    }

    public static long count(String key, Duration window) {
        return count(key, Instant.now().minus(window));
    }

    public static long count(String key, Instant from) {
        return countBetween(key, from.toEpochMilli(), Instant.now().toEpochMilli());
    }

    public static long count(String key, Date from) {
        return countBetween(key, from.getTime(), System.currentTimeMillis());
    }

    public static long countBetween(String key, long from, long to) {
        var set = getSet(key);
        if (set == null) return 0;
        return set.count(from, true, to, true);
    }

    public static boolean exceeds(String key, Duration window, long threshold) {
        return count(key, window) > threshold;
    }

    public static boolean exceeds(String key, Instant from, long threshold) {
        return count(key, from) > threshold;
    }

    public static boolean exceeds(String key, Date from, long threshold) {
        return count(key, from) > threshold;
    }

    public static void clear(String key) {
        var set = getSet(key);
        if (set != null) set.delete();
    }

    private static RScoredSortedSet<String> getSet(String key) {
        if (redis() == null) return null;
        return redis().getScoredSortedSet(KEY_PREFIX + key);
    }

    private static RedissonClient redis() {
        if (redisson == null) {
            try {
                redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
            } catch (Exception ignored) {}
        }
        return redisson;
    }
}
