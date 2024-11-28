package net.trellisframework.data.caffeine.config;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.stream.Collectors;

public class WildcardCaffeineCacheManager extends CaffeineCacheManager {

    @Override
    protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return new WildcardCaffeineCache(name, cache, isAllowNullValues());
    }

    public static class WildcardCaffeineCache extends CaffeineCache {

        public WildcardCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache, boolean allowNullValues) {
            super(name, cache, allowNullValues);
        }

        @Override
        public void evict(Object key) {
            if (key instanceof String v && v.contains("*")) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> cache = getNativeCache();
                cache.asMap().keySet().stream().filter(k -> k instanceof String x && x.matches(v.replace("*", ".*"))).collect(Collectors.toSet()).forEach(cache::invalidate);
            } else {
                super.evict(key);
            }
        }
    }
}