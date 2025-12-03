package net.trellisframework.data.redis.ratelimit;

import lombok.RequiredArgsConstructor;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.http.exception.PreConditionRequiredException;
import org.redisson.api.RJsonBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


@Component
@RequiredArgsConstructor
public class AdvancedRateLimiter {
    private static final String KEY_PREFIX = "rate-limiter:";
    private static final Map<String, PoolConfig<?>> pools = new ConcurrentHashMap<>();
    private static final Map<String, ResourceState> localCache = new ConcurrentHashMap<>();
    private static RedissonClient redisson;

    private static RedissonClient getRedisson() {
        if (redisson == null) {
            try {
                redisson = ApplicationContextProvider.context.getBean(RedissonClient.class);
            } catch (Exception ignored) {
            }
        }
        return redisson;
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

        List<T> resources = pool.resources;
        if (resources.isEmpty())
            return null;

        RateLimit targetLimits = (pool.targetLimits != null && target != null) ? pool.targetLimits.get(target) : null;

        var startIdx = pool.roundRobin.getAndIncrement() % resources.size();
        for (int i = 0; i < resources.size(); i++) {
            T resource = resources.get((startIdx + i) % resources.size());
            String resourceKey = KEY_PREFIX + poolName + ":" + pool.key.apply(resource);
            String targetKey = target != null ? resourceKey + ":" + target : null;

            synchronized ((resourceKey + (targetKey != null ? targetKey : "")).intern()) {
                long now = System.currentTimeMillis();
                if (pool.resourceLimits != null) {
                    ResourceState resourceState = getState(resourceKey, pool.resourceLimits);
                    cleanupExpiredPermits(resourceState, pool.resourceLimits, now);
                    if (!canAcquire(resourceState, pool.resourceLimits, now))
                        continue;
                }
                if (targetLimits != null) {
                    ResourceState targetState = getState(targetKey, targetLimits);
                    cleanupExpiredPermits(targetState, targetLimits, now);
                    if (!canAcquire(targetState, targetLimits, now))
                        continue;
                }
                if (pool.resourceLimits != null) {
                    ResourceState resourceState = getState(resourceKey, pool.resourceLimits);
                    recordAcquire(resourceState, pool.resourceLimits, now);
                    setState(resourceKey, resourceState, pool.resourceLimits);
                }

                if (targetLimits != null) {
                    ResourceState targetState = getState(targetKey, targetLimits);
                    recordAcquire(targetState, targetLimits, now);
                    setState(targetKey, targetState, targetLimits);
                }

                return new RateLimitResource<>(resourceKey, targetKey, pool.resourceLimits, targetLimits, resource);
            }
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
            throw new PreConditionRequiredException(Messages.POOL_NOT_REGISTERED.getMessage() + ": " + poolName);

        List<T> resources = pool.resources;
        if (resources.isEmpty())
            return false;

        RateLimit targetLimits = (pool.targetLimits != null && target != null) ? pool.targetLimits.get(target) : null;

        for (int i = 0; i < resources.size(); i++) {
            T resource = resources.get(i);
            String resourceKey = KEY_PREFIX + poolName + ":" + pool.key.apply(resource);
            String targetKey = target != null ? resourceKey + ":" + target : null;

            synchronized ((resourceKey + (targetKey != null ? targetKey : "")).intern()) {
                long now = System.currentTimeMillis();
                boolean resourceAvailable = true;
                boolean targetAvailable = true;

                if (pool.resourceLimits != null) {
                    ResourceState resourceState = getState(resourceKey, pool.resourceLimits);
                    cleanupExpiredPermits(resourceState, pool.resourceLimits, now);
                    resourceAvailable = canAcquire(resourceState, pool.resourceLimits, now);
                }

                if (resourceAvailable && targetLimits != null) {
                    ResourceState targetState = getState(targetKey, targetLimits);
                    cleanupExpiredPermits(targetState, targetLimits, now);
                    targetAvailable = canAcquire(targetState, targetLimits, now);
                }

                if (resourceAvailable && targetAvailable)
                    return true;
            }
        }
        return false;
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

    static void releaseResource(String key, RateLimit limits) {
        if (limits == null || key == null) return;
        synchronized (key.intern()) {
            var state = getState(key, limits);
            if (!state.getAcquiredTimestamps().isEmpty())
                state.getAcquiredTimestamps().removeFirst();

            if (isStateEmpty(state))
                deleteState(key);
            else
                setState(key, state, limits);
        }
    }

    private static boolean isStateEmpty(ResourceState state) {
        return state.getAcquiredTimestamps().isEmpty()
                && state.getRates().isEmpty()
                && state.getCoolOffUntil() == null;
    }

    private static void deleteState(String key) {
        localCache.remove(key);
        if (getRedisson() != null) {
            try {
                getRedisson().getBucket(key).delete();
            } catch (Exception e) {
                Logger.warn("Failed to delete from Redis: " + e.getMessage());
            }
        }
    }

    private static ResourceState getState(String key, RateLimit limits) {
        ResourceState state = null;

        if (getRedisson() != null) {
            try {
                RJsonBucket<ResourceState> bucket = getRedisson().getJsonBucket(key, new JacksonCodec<>(ResourceState.class));
                state = bucket.get();
            } catch (Exception e) {
                Logger.warn("Failed to get from Redis: " + e.getMessage());
            }
        }

        if (state == null)
            state = localCache.get(key);

        if (state != null) {
            refreshWindows(state, limits);
            return state;
        }

        return createNewState(limits);
    }

    private static void refreshWindows(ResourceState state, RateLimit limits) {
        var now = System.currentTimeMillis();
        for (var window : state.getRates()) {
            if (now - window.getStartAt() >= window.getDuration()) {
                window.setStartAt(now);
                window.setUsed(0);
            }
        }
    }

    private static ResourceState createNewState(RateLimit limits) {
        var state = new ResourceState();
        var now = System.currentTimeMillis();
        for (var rate : limits.getRates())
            state.getRates().add(ResourceState.Window.of(rate.getDuration().toMillis(), now, 0));
        return state;
    }

    private static void setState(String key, ResourceState state, RateLimit limits) {
        localCache.put(key, state);
        if (getRedisson() != null) {
            try {
                long maxDuration = limits.getRates().stream().mapToLong(r -> r.getDuration().toMillis()).max().orElse(86400_000L);
                RJsonBucket<ResourceState> bucket = getRedisson().getJsonBucket(key, new JacksonCodec<>(ResourceState.class));
                bucket.set(state);
                bucket.expire(Duration.ofMillis(maxDuration + 60_000L));
            } catch (Exception e) {
                Logger.warn("Failed to save to Redis: " + e.getMessage());
            }
        }
    }

    static void applyCoolOff(String key, Duration duration, RateLimit limits) {
        if (key == null || limits == null) return;
        synchronized (key.intern()) {
            var state = getState(key, limits);
            state.setCoolOffUntil(System.currentTimeMillis() + duration.toMillis());
            setState(key, state, limits);
        }
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
