package net.trellisframework.data.redis.config;

import lombok.Getter;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
public class WildcardRedisCacheManager extends RedisCacheManager {

    private final RedisCacheWriter cacheWriter;

    public WildcardRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, Map<String, RedisCacheConfiguration> initialCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfiguration);
        this.cacheWriter = cacheWriter;
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        return new WildcardRedisCache(name, getCacheWriter(), cacheConfig);
    }

    public static class WildcardRedisCache extends RedisCache {

        protected WildcardRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
            super(name, cacheWriter, cacheConfig);
        }

        @Override
        public void evict(Object key) {
            if (key instanceof String v && v.contains("*")) {
                byte[] pattern = (getCacheConfiguration().getKeyPrefixFor(getName()) + v).getBytes(StandardCharsets.UTF_8);
                getCacheWriter().clean(getName(), pattern);
            } else {
                super.evict(key);
            }
        }
    }
}
