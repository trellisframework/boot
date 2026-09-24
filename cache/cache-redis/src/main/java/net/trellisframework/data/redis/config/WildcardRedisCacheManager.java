package net.trellisframework.data.redis.config;

import lombok.Getter;
import net.trellisframework.core.log.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Getter
@NullMarked
public class WildcardRedisCacheManager extends RedisCacheManager {

    private final RedisCacheWriter cacheWriter;

    public WildcardRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfiguration);
        this.cacheWriter = cacheWriter;
    }

    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        return new WildcardRedisCache(name, getCacheWriter(), cacheConfig != null ? cacheConfig : getDefaultCacheConfiguration());
    }

    public static class WildcardRedisCache extends RedisCache {

        protected WildcardRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
            super(name, cacheWriter, cacheConfig);
        }

        @Override
        protected @Nullable Object lookup(Object key) {
            try {
                return super.lookup(key);
            } catch (SerializationException e) {
                evictUnreadable(key, e);
                return null;
            }
        }

        @Override
        public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
            try {
                return super.get(key, valueLoader);
            } catch (SerializationException e) {
                evictUnreadable(key, e);
                return load(key, valueLoader);
            }
        }

        private <T> T load(Object key, Callable<T> valueLoader) {
            try {
                T value = valueLoader.call();
                put(key, value);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public CompletableFuture<@Nullable ValueWrapper> retrieve(Object key) {
            return super.retrieve(key).<@Nullable ValueWrapper>handle((value, e) -> {
                if (e == null)
                    return value;
                Throwable cause = e instanceof CompletionException ? e.getCause() : e;
                if (!(cause instanceof SerializationException failure))
                    throw new CompletionException(cause);
                evictUnreadable(key, failure);
                return null;
            });
        }

        private void evictUnreadable(Object key, SerializationException e) {
            Logger.warn("Evicting unreadable cache entry " + getName() + ":" + key + ": " + e.getMessage());
            evict(key);
        }

        @Override
        public void evict(Object key) {
            if (key instanceof String v && v.contains("*")) {
                byte[] pattern = (getCacheConfiguration().getKeyPrefixFor(getName()) + v).getBytes(StandardCharsets.UTF_8);
                getCacheWriter().clear(getName(), pattern);
            } else {
                super.evict(key);
            }
        }
    }
}
