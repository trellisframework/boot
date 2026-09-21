package net.trellisframework.data.redis.ratelimit;

import lombok.RequiredArgsConstructor;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Limited;
import net.trellisframework.data.redis.ratelimit.RateLimiterScript.Op;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.http.exception.PreConditionRequiredException;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class AdvancedRateLimiter {
    private static final String KEY_PREFIX = "rate-limiter:";
    private static final long REDISSON_LOOKUP_RETRY_MILLIS = 5_000L;
    private static final long FALLBACK_WARN_MIN_BACKOFF_MILLIS = 1_000L;
    private static final long FALLBACK_WARN_MAX_BACKOFF_MILLIS = 300_000L;
    private static final int FALLBACK_WARN_MAX_TRACKED_KEYS = 10_000;

    private static final Map<String, PoolConfig<?>> pools = new ConcurrentHashMap<>();
    private static final Map<String, ResourceState> localCache = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();
    private static final Map<String, RateLimit> rateLimitOverrides = new ConcurrentHashMap<>();
    private static final Map<String, Backoff> fallbackWarnings = new ConcurrentHashMap<>();
    private static volatile RedissonClient redisson;
    private static volatile long nextRedissonLookupAt;

    private static RedissonClient redis() {
        RedissonClient client = redisson;
        if (client != null)
            return client;
        long now = System.currentTimeMillis();
        if (now < nextRedissonLookupAt)
            return null;
        try {
            client = ApplicationContextProvider.context.getBean(RedissonClient.class);
            redisson = client;
            return client;
        } catch (Exception e) {
            nextRedissonLookupAt = now + REDISSON_LOOKUP_RETRY_MILLIS;
            return null;
        }
    }

    static void reset() {
        pools.clear();
        localCache.clear();
        localLocks.clear();
        rateLimitOverrides.clear();
        fallbackWarnings.clear();
        redisson = null;
        nextRedissonLookupAt = 0;
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
        if (resources.isEmpty())
            return null;

        var startIdx = Math.floorMod(pool.roundRobin.getAndIncrement(), resources.size());
        for (int i = 0; i < resources.size(); i++) {
            T resource = resources.get((startIdx + i) % resources.size());
            String resourceKey = KEY_PREFIX + poolName + ":" + pool.key.apply(resource);
            String targetKey = target != null ? resourceKey + ":" + target : null;
            RateLimit effectiveResourceLimits = effectiveResourceLimits(pool, resourceKey);
            RateLimit effectiveTargetLimits = effectiveTargetLimits(pool, target, targetKey);

            if (execute(Op.ACQUIRE, limited(resourceKey, effectiveResourceLimits, targetKey, effectiveTargetLimits), 0))
                return new RateLimitResource<>(resourceKey, targetKey, effectiveResourceLimits, effectiveTargetLimits, resource);
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
            RateLimit effectiveResourceLimits = effectiveResourceLimits(pool, resourceKey);
            RateLimit effectiveTargetLimits = effectiveTargetLimits(pool, target, targetKey);

            if (execute(Op.CHECK, limited(resourceKey, effectiveResourceLimits, targetKey, effectiveTargetLimits), 0))
                return true;
        }
        return false;
    }

    private static RateLimit effectiveResourceLimits(PoolConfig<?> pool, String resourceKey) {
        return rateLimitOverrides.getOrDefault(resourceKey, pool.resourceLimits);
    }

    private static RateLimit effectiveTargetLimits(PoolConfig<?> pool, String target, String targetKey) {
        if (targetKey == null)
            return null;
        RateLimit limits = rateLimitOverrides.get(targetKey);
        if (limits == null && pool.targetLimits != null)
            limits = pool.targetLimits.get(target);
        return limits;
    }

    private static List<Limited> limited(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        List<Limited> targets = new ArrayList<>(2);
        if (resourceKey != null && resourceLimits != null)
            targets.add(new Limited(resourceKey, resourceLimits));
        if (targetKey != null && targetLimits != null)
            targets.add(new Limited(targetKey, targetLimits));
        return targets;
    }

    static void releaseResource(String key, RateLimit limits) {
        if (limits == null || key == null) return;
        execute(Op.RELEASE, List.of(new Limited(key, limits)), 0);
    }

    static void applyCoolOff(String key, Duration duration, RateLimit limits) {
        if (key == null || limits == null) return;
        execute(Op.COOL_OFF, List.of(new Limited(key, limits)), duration.toMillis());
    }

    static boolean canAcquireResource(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        return execute(Op.CHECK, limited(resourceKey, resourceLimits, targetKey, targetLimits), 0);
    }

    static boolean tryAcquireResource(String resourceKey, RateLimit resourceLimits, String targetKey, RateLimit targetLimits) {
        return execute(Op.ACQUIRE, limited(resourceKey, resourceLimits, targetKey, targetLimits), 0);
    }

    private static boolean execute(Op op, List<Limited> targets, long coolOffMillis) {
        if (targets.isEmpty())
            return true;
        long now = System.currentTimeMillis();
        RedissonClient client = redis();
        if (client != null) {
            try {
                return RateLimiterScript.execute(client, op, targets, now, coolOffMillis);
            } catch (Exception e) {
                warnFallback(targets.getFirst().key(), e);
            }
        }
        return executeLocally(op, targets, now, coolOffMillis);
    }

    private static void warnFallback(String key, Exception e) {
        long now = System.currentTimeMillis();
        if (fallbackWarnings.size() > FALLBACK_WARN_MAX_TRACKED_KEYS)
            fallbackWarnings.clear();
        boolean[] fire = {false};
        fallbackWarnings.compute(key, (k, previous) -> {
            if (previous == null || now >= previous.quietSince())
                return fired(fire, now, FALLBACK_WARN_MIN_BACKOFF_MILLIS);
            if (now >= previous.nextWarnAt())
                return fired(fire, now, Math.min(previous.interval() * 2, FALLBACK_WARN_MAX_BACKOFF_MILLIS));
            return previous;
        });
        if (fire[0])
            Logger.warn("AdvancedRateLimiter", "Redis unavailable for " + key + ", using local fallback: " + e.getMessage());
    }

    private static Backoff fired(boolean[] fire, long now, long interval) {
        fire[0] = true;
        return new Backoff(now + interval, interval);
    }

    record Backoff(long nextWarnAt, long interval) {
        long quietSince() {
            return nextWarnAt + 4 * interval;
        }
    }

    private static boolean executeLocally(Op op, List<Limited> targets, long now, long coolOffMillis) {
        List<ReentrantLock> locks = new ArrayList<>(targets.size());
        for (Limited target : targets)
            locks.add(localLocks.computeIfAbsent(target.key(), k -> new ReentrantLock()));
        locks.forEach(ReentrantLock::lock);
        try {
            List<ResourceState> states = new ArrayList<>(targets.size());
            for (Limited target : targets)
                states.add(loadLocal(target.key(), target.limits(), now));
            switch (op) {
                case ACQUIRE, CHECK -> {
                    for (int i = 0; i < targets.size(); i++)
                        if (!canAcquire(states.get(i), targets.get(i).limits(), now))
                            return false;
                    if (op == Op.ACQUIRE)
                        for (int i = 0; i < targets.size(); i++) {
                            recordAcquire(states.get(i), targets.get(i).limits(), now);
                            saveLocal(targets.get(i).key(), states.get(i));
                        }
                    return true;
                }
                case RELEASE -> {
                    for (int i = 0; i < targets.size(); i++) {
                        if (!states.get(i).getAcquiredTimestamps().isEmpty())
                            states.get(i).getAcquiredTimestamps().removeFirst();
                        saveLocal(targets.get(i).key(), states.get(i));
                    }
                    return true;
                }
                case COOL_OFF -> {
                    for (int i = 0; i < targets.size(); i++) {
                        states.get(i).setCoolOffUntil(now + coolOffMillis);
                        saveLocal(targets.get(i).key(), states.get(i));
                    }
                    return true;
                }
            }
            return false;
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--)
                locks.get(i).unlock();
        }
    }

    private static ResourceState loadLocal(String key, RateLimit limits, long now) {
        ResourceState stored = localCache.get(key);
        ResourceState state = new ResourceState();
        for (var rate : limits.getRates()) {
            long duration = rate.getDuration().toMillis();
            ResourceState.Window window = stored == null ? null : stored.getRates().stream()
                    .filter(w -> w.getDuration() == duration).findFirst().orElse(null);
            if (window == null || now - window.getStartAt() >= duration)
                window = ResourceState.Window.of(duration, now, 0);
            else
                window = ResourceState.Window.of(duration, window.getStartAt(), window.getUsed());
            state.getRates().add(window);
        }
        if (stored != null) {
            long permitTimeout = RateLimiterScript.permitTimeoutMillis(limits);
            for (Long ts : stored.getAcquiredTimestamps())
                if (limits.getMaxConcurrent() <= 0 || permitTimeout <= 0 || now - ts < permitTimeout)
                    state.getAcquiredTimestamps().add(ts);
            state.setCoolOffUntil(stored.getCoolOffUntil());
        }
        return state;
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
        state.setCoolOffUntil(null);
        if (limits.getMaxConcurrent() > 0)
            state.getAcquiredTimestamps().add(now);
        for (var window : state.getRates())
            window.setUsed(window.getUsed() + 1);
    }

    private static void saveLocal(String key, ResourceState state) {
        if (state.getAcquiredTimestamps().isEmpty() && state.getRates().isEmpty() && state.getCoolOffUntil() == null)
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
