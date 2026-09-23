package net.trellisframework.data.redis.ratelimit;

import lombok.RequiredArgsConstructor;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Limited;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Op;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.http.exception.PreConditionRequiredException;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


@Component
public class AdvancedRateLimiter {
    private static final String KEY_PREFIX = "rate-limiter:";
    private static final Map<String, PoolConfig<?>> pools = new ConcurrentHashMap<>();
    private static final Map<String, ResourceState> localCache = new ConcurrentHashMap<>();
    private static final Map<String, Lock> localLocks = new ConcurrentHashMap<>();
    private static final Map<String, RateLimit> rateLimitOverrides = new ConcurrentHashMap<>();
    private static final long WARN_INTERVAL_MILLIS = 5_000L;
    private static volatile RedissonClient redisson;
    private static volatile long warnPausedUntil;

    public AdvancedRateLimiter(ObjectProvider<RedissonClient> client) {
        redisson = client.getIfAvailable();
        if (redisson == null)
            Logger.warn("No RedissonClient available, rate limits will be enforced per JVM instead of fleet-wide");
    }

    static void reset() {
        pools.clear();
        localCache.clear();
        localLocks.clear();
        rateLimitOverrides.clear();
        redisson = null;
        warnPausedUntil = 0;
    }

    public static <T> PoolBuilder<T> pool(String poolName) {
        return new PoolBuilder<>(poolName);
    }

    public static boolean exists(String poolName) {
        return pools.containsKey(poolName);
    }

    @SuppressWarnings("unchecked")
    public static <T> void setResources(String poolName, Collection<T> resources) {
        var pool = (PoolConfig<T>) pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        pool.resources.clear();
        pool.resources.addAll(resources);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setResourceLimits(String poolName, RateLimit limits) {
        var pool = pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        pools.put(poolName, new PoolConfig(pool.resources, limits, pool.targetLimits, pool.roundRobin, pool.key));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setTargetLimits(String poolName, TargetLimits targetLimits) {
        var pool = pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        pools.put(poolName, new PoolConfig(pool.resources, pool.resourceLimits, targetLimits, pool.roundRobin, pool.key));
    }

    public static void putTargetLimit(String poolName, String target, RateLimit rateLimit) {
        var pool = pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        getOrCreateTargetLimits(poolName, pool).put(target, rateLimit);
    }

    public static void putTargetLimitIfAbsent(String poolName, String target, RateLimit rateLimit) {
        var pool = pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        getOrCreateTargetLimits(poolName, pool).putIfAbsent(target, rateLimit);
    }

    public static void removeTargetLimit(String poolName, String target) {
        var pool = pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);
        if (pool.targetLimits == null)
            return;
        pool.targetLimits.remove(target);
    }

    public static boolean containsTargetLimit(String poolName, String target) {
        var pool = pools.get(poolName);
        if (pool == null)
            return false;
        if (pool.targetLimits == null)
            return false;
        return pool.targetLimits.contains(target);
    }

    static void setRateLimitOverride(String key, RateLimit limits) {
        if (limits == null)
            rateLimitOverrides.remove(key);
        else
            rateLimitOverrides.put(key, limits);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TargetLimits getOrCreateTargetLimits(String poolName, PoolConfig<?> pool) {
        if (pool.targetLimits != null)
            return pool.targetLimits;
        var newTargetLimits = TargetLimits.create();
        pools.put(poolName, new PoolConfig(pool.resources, pool.resourceLimits, newTargetLimits, pool.roundRobin, pool.key));
        return newTargetLimits;
    }

    public static <T> RateLimitResource<T> acquire(String poolName) {
        return acquire(poolName, null);
    }

    public static <T> RateLimitResource<T> acquire(String poolName, String target) {
        RateLimitResource<T> resource = tryAcquire(poolName, target);
        if (resource == null)
            throw new NotFoundException(Messages.NO_AVAILABLE_RESOURCES_IN_POOL.getMessage() + ": " + poolName);
        return resource;
    }

    public static <T> RateLimitResource<T> tryAcquire(String poolName) {
        return tryAcquire(poolName, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> RateLimitResource<T> tryAcquire(String poolName, String target) {
        var pool = (PoolConfig<T>) pools.get(poolName);
        if (pool == null)
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);

        List<T> resources = List.copyOf(pool.resources);
        int count = resources.size();
        if (count == 0)
            return null;

        var startIdx = Math.floorMod(pool.roundRobin.getAndIncrement(), count);
        for (int i = 0; i < count; i++) {
            T resource = resources.get((startIdx + i) % count);
            String resourceKey = KEY_PREFIX + poolName + ":" + pool.key.apply(resource);
            String targetKey = target != null ? resourceKey + ":" + target : null;

            RateLimit effectiveResourceLimits = rateLimitOverrides.getOrDefault(resourceKey, pool.resourceLimits);
            RateLimit effectiveTargetLimits = targetKey != null ? rateLimitOverrides.get(targetKey) : null;
            if (effectiveTargetLimits == null && pool.targetLimits != null && target != null)
                effectiveTargetLimits = pool.targetLimits.get(target);

            String permitId = execute(Op.ACQUIRE, limited(resourceKey, effectiveResourceLimits, targetKey, effectiveTargetLimits), null);
            if (permitId != null)
                return new RateLimitResource<>(resourceKey, targetKey, effectiveResourceLimits, effectiveTargetLimits, resource).permit(permitId);
        }
        return null;
    }

    public static boolean canAcquire(String poolName) {
        return canAcquire(poolName, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean canAcquire(String poolName, String target) {
        var pool = (PoolConfig<T>) pools.get(poolName);
        if (pool == null)
            return false;

        for (T resource : pool.resources) {
            String resourceKey = KEY_PREFIX + poolName + ":" + pool.key.apply(resource);
            String targetKey = target != null ? resourceKey + ":" + target : null;

            RateLimit effectiveResourceLimits = rateLimitOverrides.getOrDefault(resourceKey, pool.resourceLimits);
            RateLimit effectiveTargetLimits = targetKey != null ? rateLimitOverrides.get(targetKey) : null;
            if (effectiveTargetLimits == null && pool.targetLimits != null && target != null)
                effectiveTargetLimits = pool.targetLimits.get(target);

            if (execute(Op.CHECK, limited(resourceKey, effectiveResourceLimits, targetKey, effectiveTargetLimits), null) != null)
                return true;
        }
        return false;
    }

    static void releaseResource(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits, String permitId) {
        if (permitId == null)
            return;
        execute(Op.RELEASE, limited(resourceKey, resourceLimits, targetKey, targetLimits), permitId);
    }

    static void applyCoolOff(String key, Duration duration, RateLimit limits) {
        execute(Op.COOL_OFF, limited(key, limits, null, null), Long.toString(duration.toMillis()));
    }

    static boolean canAcquireResource(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        return execute(Op.CHECK, limited(resourceKey, resourceLimits, targetKey, targetLimits), null) != null;
    }

    static String tryAcquireResource(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        return execute(Op.ACQUIRE, limited(resourceKey, resourceLimits, targetKey, targetLimits), null);
    }

    private static List<Limited> limited(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        List<Limited> targets = new ArrayList<>(2);
        if (resourceKey != null && resourceLimits != null)
            targets.add(new Limited("{" + resourceKey + "}", resourceLimits));
        if (targetKey != null && targetLimits != null)
            targets.add(new Limited("{" + resourceKey + "}" + targetKey.substring(resourceKey.length()), targetLimits));
        return targets;
    }

    private static String execute(Op op, List<Limited> targets, String arg) {
        if (targets.isEmpty())
            return "";
        long now = System.currentTimeMillis();
        String permitArg = op == Op.ACQUIRE ? newPermitId(now) : arg == null ? "" : arg;
        RedissonClient client = redisson;
        if (client != null)
            try {
                return outcome(RateLimiterScript.execute(client, op, targets, now, permitArg), op, permitArg);
            } catch (Exception e) {
                warnPaused(now, e);
            }
        return outcome(executeLocally(op, targets, now, permitArg), op, permitArg);
    }

    private static String outcome(boolean allowed, Op op, String permitArg) {
        if (!allowed)
            return null;
        return op == Op.ACQUIRE ? permitArg : "";
    }

    private static String newPermitId(long now) {
        return now + "-" + Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36);
    }

    private static Long permitTimestamp(String permitId) {
        int dash = permitId.indexOf('-');
        if (dash <= 0)
            return null;
        try {
            return Long.parseLong(permitId.substring(0, dash));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void warnPaused(long now, Exception e) {
        if (now < warnPausedUntil)
            return;
        warnPausedUntil = now + WARN_INTERVAL_MILLIS;
        Logger.warn("Failed to run rate limiter in Redis, using local state: " + e.getMessage());
    }

    private static boolean executeLocally(Op op, List<Limited> targets, long now, String arg) {
        List<Lock> locks = targets.stream().map(target -> localLocks.computeIfAbsent(target.key(), k -> new ReentrantLock())).toList();
        locks.forEach(Lock::lock);
        try {
            List<ResourceState> states = targets.stream().map(target -> getState(target.key(), target.limits(), now)).toList();
            for (int i = 0; i < targets.size(); i++) {
                ResourceState state = states.get(i);
                RateLimit limits = targets.get(i).limits();
                cleanupExpiredPermits(state, limits, now);
                switch (op) {
                    case RELEASE -> {
                        Long acquiredAt = permitTimestamp(arg);
                        if (acquiredAt != null)
                            state.getAcquiredTimestamps().remove(acquiredAt);
                    }
                    case COOL_OFF -> state.setCoolOffUntil(now + Long.parseLong(arg));
                    default -> {
                        if (!canAcquire(state, limits, now))
                            return false;
                    }
                }
            }
            if (op == Op.CHECK)
                return true;
            for (int i = 0; i < targets.size(); i++) {
                if (op == Op.ACQUIRE)
                    recordAcquire(states.get(i), targets.get(i).limits(), now);
                setState(targets.get(i).key(), states.get(i));
            }
            return true;
        } finally {
            locks.reversed().forEach(Lock::unlock);
        }
    }

    private static void cleanupExpiredPermits(ResourceState state, RateLimit limits, long now) {
        if (limits.getMaxConcurrent() <= 0 || limits.getPermitTimeout() == null) return;
        state.getAcquiredTimestamps().removeIf(ts -> now - ts >= limits.getPermitTimeout().toMillis());
    }

    private static boolean canAcquire(ResourceState state, RateLimit limits, long now) {
        if (state.getCoolOffUntil() != null && now < state.getCoolOffUntil())
            return false;
        if (limits.getMaxConcurrent() > 0 && state.getAcquiredTimestamps().size() >= limits.getMaxConcurrent())
            return false;
        for (var window : state.getRates()) {
            var rate = limits.getRates().stream()
                    .filter(r -> r.getDuration().toMillis() == window.getDuration())
                    .findFirst();
            if (rate.isPresent() && window.getUsed() >= rate.get().getMaxRequests())
                return false;
        }
        return true;
    }

    private static void recordAcquire(ResourceState state, RateLimit limits, long now) {
        if (state.getCoolOffUntil() != null)
            state.setCoolOffUntil(null);
        if (limits.getMaxConcurrent() > 0)
            state.getAcquiredTimestamps().add(now);
        for (var window : state.getRates()) {
            if (now - window.getStartAt() >= window.getDuration()) {
                window.setStartAt(now);
                window.setUsed(0);
            }
            window.setUsed(window.getUsed() + 1);
        }
    }

    private static boolean isStateEmpty(ResourceState state) {
        return state.getAcquiredTimestamps().isEmpty()
                && state.getRates().isEmpty()
                && state.getCoolOffUntil() == null;
    }

    private static ResourceState getState(String key, RateLimit limits, long now) {
        ResourceState state = localCache.getOrDefault(key, new ResourceState());
        for (var window : state.getRates()) {
            if (now - window.getStartAt() >= window.getDuration()) {
                window.setStartAt(now);
                window.setUsed(0);
            }
        }
        for (var rate : limits.getRates())
            if (state.getRates().stream().noneMatch(window -> window.getDuration() == rate.getDuration().toMillis()))
                state.getRates().add(ResourceState.Window.of(rate.getDuration().toMillis(), now, 0));
        return state;
    }

    private static void setState(String key, ResourceState state) {
        if (isStateEmpty(state))
            localCache.remove(key);
        else
            localCache.put(key, state);
    }

    @RequiredArgsConstructor
    public static class PoolBuilder<T> {
        private final String poolName;
        private Collection<T> resources;
        private Function<T, String> key = Object::toString;
        private RateLimit resourceLimits;
        private TargetLimits targetLimits;
        private boolean allowOverwrite;

        public PoolBuilder<T> resources(Collection<T> resources) {
            this.resources = resources;
            return this;
        }

        public PoolBuilder<T> key(Function<T, String> key) {
            this.key = key;
            return this;
        }

        public PoolBuilder<T> resourceLimits(RateLimit limits) {
            this.resourceLimits = limits;
            return this;
        }

        public PoolBuilder<T> targetLimits(TargetLimits targetLimits) {
            this.targetLimits = targetLimits;
            return this;
        }

        public PoolBuilder<T> allowOverwrite() {
            this.allowOverwrite = true;
            return this;
        }

        public void build() {
            if (resources == null)
                resources = new CopyOnWriteArrayList<>();
            var config = new PoolConfig<>(new CopyOnWriteArrayList<>(resources), resourceLimits, targetLimits, new AtomicInteger(0), key);
            if (allowOverwrite)
                pools.put(poolName, config);
            else
                pools.putIfAbsent(poolName, config);
        }
    }

    record PoolConfig<T>(CopyOnWriteArrayList<T> resources, RateLimit resourceLimits, TargetLimits targetLimits, AtomicInteger roundRobin, Function<T, String> key) {
    }
}
