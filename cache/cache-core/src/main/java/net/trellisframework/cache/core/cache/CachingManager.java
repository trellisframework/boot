package net.trellisframework.cache.core.cache;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

public class CachingManager {

    private static CacheManager getInstance() {
        return ApplicationContextProvider.context.getBean(CacheManager.class);
    }

    public static Optional<Cache> getCache(String name) {
        return Optional.ofNullable(getInstance().getCache(name));
    }

    public static void put(String name, Object key, Object value) {
        getCache(name).ifPresent(x -> x.put(key, value));
    }

    public static void putIfAbsent(String name, Object key, Object value) {
        getCache(name).ifPresent(x -> x.putIfAbsent(key, value));
    }

    public static <T> Optional<T> get(String name, Object key, Class<T> clazz) {
        return getCache(name).map(x -> x.get(key, clazz));
    }

    public static void evict(String name) {
        getCache(name).ifPresent(Cache::clear);
    }

    public static void evict(String name, String key) {
        getCache(name).ifPresent(x -> x.evict(key));
    }

}
