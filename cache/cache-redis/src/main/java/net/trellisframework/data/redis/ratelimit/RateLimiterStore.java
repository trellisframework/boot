package net.trellisframework.data.redis.ratelimit;

import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class RateLimiterStore {

    record Limited(String key, RateLimit limits) {
    }

    private static final String WINDOW = "#w:";
    private static final String PERMITS = "#p";
    private static final String COOL_OFF = "#c";
    static final long DEFAULT_TTL_MILLIS = 86400_000L;
    static final long TTL_GRACE_MILLIS = 60_000L;

    private RateLimiterStore() {
    }

    static boolean check(RedissonClient client, List<Limited> targets, long now) {
        Batch batch = new Batch(client);
        List<Reading> readings = new ArrayList<>(targets.size());
        for (Limited target : targets)
            readings.add(new Reading(batch, target, now));
        batch.execute();
        for (Reading reading : readings)
            if (!reading.available(now))
                return false;
        return true;
    }

    static boolean acquire(RedissonClient client, List<Limited> targets, long now) {
        if (!check(client, targets, now))
            return false;
        Batch batch = new Batch(client);
        List<Claim> claims = new ArrayList<>(targets.size());
        for (Limited target : targets)
            claims.add(new Claim(batch, target, now));
        batch.execute();
        boolean granted = true;
        for (Claim claim : claims)
            granted &= claim.granted(now);
        Batch followUp = new Batch(client);
        for (Claim claim : claims)
            if (granted)
                claim.repair(followUp);
            else
                claim.rollback(followUp);
        if (followUp.execute() && !granted) {
            Batch cleanup = new Batch(client);
            for (Claim claim : claims)
                claim.dropNegative(cleanup);
            cleanup.execute();
        }
        return granted;
    }

    static void release(RedissonClient client, Limited target, long now) {
        Batch batch = new Batch(client);
        RScoredSortedSetAsync<String> permits = batch.permits(target.key());
        long timeout = permitTimeoutMillis(target.limits());
        if (timeout > 0)
            batch.add(permits.removeRangeByScoreAsync(Double.NEGATIVE_INFINITY, true, now - timeout, true));
        batch.add(permits.pollFirstAsync());
        batch.execute();
    }

    static void coolOff(RedissonClient client, Limited target, long now, long millis) {
        client.getBucket(target.key() + COOL_OFF, LongCodec.INSTANCE).set(now + millis, Duration.ofMillis(millis));
    }

    static long permitTimeoutMillis(RateLimit limits) {
        return limits.getPermitTimeout() == null ? 0 : limits.getPermitTimeout().toMillis();
    }

    static long ttlMillis(RateLimit limits) {
        long windows = limits.getRates().stream().mapToLong(r -> r.getDuration().toMillis()).max().orElse(DEFAULT_TTL_MILLIS);
        return Math.max(windows, permitTimeoutMillis(limits)) + TTL_GRACE_MILLIS;
    }

    private static String windowKey(String key, RateLimit.Rate rate) {
        return key + WINDOW + rate.getDuration().toMillis();
    }

    private static final class Batch {
        private final RBatch batch;
        private int size;
        private List<?> responses;

        Batch(RedissonClient client) {
            batch = client.createBatch(BatchOptions.defaults());
        }

        int add(Object command) {
            return size++;
        }

        boolean execute() {
            if (size == 0)
                return false;
            responses = batch.execute().getResponses();
            return true;
        }

        @SuppressWarnings("unchecked")
        <T> T get(int index) {
            return (T) responses.get(index);
        }

        long count(int index) {
            Object value = get(index);
            return value == null ? 0 : ((Number) value).longValue();
        }

        RScoredSortedSetAsync<String> permits(String key) {
            return batch.getScoredSortedSet(key + PERMITS, StringCodec.INSTANCE);
        }
    }

    private static final class Reading {
        private final Batch batch;
        private final RateLimit limits;
        private final int coolOffUntil;
        private final List<Integer> windows = new ArrayList<>();
        private final int permits;

        Reading(Batch batch, Limited target, long now) {
            this.batch = batch;
            limits = target.limits();
            coolOffUntil = batch.add(batch.batch.getBucket(target.key() + COOL_OFF, LongCodec.INSTANCE).getAsync());
            for (RateLimit.Rate rate : limits.getRates())
                windows.add(batch.add(batch.batch.getBucket(windowKey(target.key(), rate), LongCodec.INSTANCE).getAsync()));
            if (limits.getMaxConcurrent() > 0) {
                RScoredSortedSetAsync<String> set = batch.permits(target.key());
                long timeout = permitTimeoutMillis(limits);
                permits = batch.add(timeout > 0 ? set.countAsync(now - timeout, false, Double.POSITIVE_INFINITY, true) : set.sizeAsync());
            } else
                permits = -1;
        }

        boolean available(long now) {
            Long until = batch.get(coolOffUntil);
            if (until != null && now < until)
                return false;
            if (permits >= 0 && batch.count(permits) >= limits.getMaxConcurrent())
                return false;
            for (int i = 0; i < windows.size(); i++)
                if (batch.count(windows.get(i)) >= limits.getRates().get(i).getMaxRequests())
                    return false;
            return true;
        }
    }

    private static final class Claim {
        private final Batch batch;
        private final String key;
        private final RateLimit limits;
        private final String member;
        private final int coolOffUntil;
        private final List<Integer> counts = new ArrayList<>();
        private final int permits;
        private Batch undo;
        private final List<Integer> decremented = new ArrayList<>();

        Claim(Batch batch, Limited target, long now) {
            this.batch = batch;
            key = target.key();
            limits = target.limits();
            member = Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
            coolOffUntil = batch.add(batch.batch.getBucket(key + COOL_OFF, LongCodec.INSTANCE).getAsync());
            for (RateLimit.Rate rate : limits.getRates()) {
                String window = windowKey(key, rate);
                batch.add(batch.batch.getBucket(window, LongCodec.INSTANCE).setIfAbsentAsync(0L, rate.getDuration()));
                counts.add(batch.add(batch.batch.getAtomicLong(window).incrementAndGetAsync()));
            }
            if (limits.getMaxConcurrent() > 0) {
                RScoredSortedSetAsync<String> set = batch.permits(key);
                long timeout = permitTimeoutMillis(limits);
                if (timeout > 0)
                    batch.add(set.removeRangeByScoreAsync(Double.NEGATIVE_INFINITY, true, now - timeout, true));
                batch.add(set.addAsync(now, member));
                permits = batch.add(set.sizeAsync());
                batch.add(set.expireAsync(Duration.ofMillis(ttlMillis(limits))));
            } else
                permits = -1;
        }

        boolean granted(long now) {
            Long until = batch.get(coolOffUntil);
            if (until != null && now < until)
                return false;
            if (permits >= 0 && batch.count(permits) > limits.getMaxConcurrent())
                return false;
            for (int i = 0; i < counts.size(); i++)
                if (batch.count(counts.get(i)) > limits.getRates().get(i).getMaxRequests())
                    return false;
            return true;
        }

        void repair(Batch followUp) {
            for (int i = 0; i < counts.size(); i++) {
                RateLimit.Rate rate = limits.getRates().get(i);
                if (batch.count(counts.get(i)) == 1L)
                    followUp.add(followUp.batch.getBucket(windowKey(key, rate)).expireAsync(rate.getDuration()));
            }
        }

        void rollback(Batch followUp) {
            undo = followUp;
            for (RateLimit.Rate rate : limits.getRates())
                decremented.add(followUp.add(followUp.batch.getAtomicLong(windowKey(key, rate)).decrementAndGetAsync()));
            if (permits >= 0)
                followUp.add(followUp.permits(key).removeAsync(member));
        }

        void dropNegative(Batch cleanup) {
            for (int i = 0; i < decremented.size(); i++)
                if (undo.count(decremented.get(i)) < 0)
                    cleanup.add(cleanup.batch.getBucket(windowKey(key, limits.getRates().get(i))).deleteAsync());
        }
    }
}
