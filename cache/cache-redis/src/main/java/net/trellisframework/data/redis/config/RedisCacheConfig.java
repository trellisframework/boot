package net.trellisframework.data.redis.config;

import net.trellisframework.boot.cache.core.constant.CacheManagers;
import net.trellisframework.boot.cache.core.payload.TTL;
import net.trellisframework.boot.cache.core.scanner.AnnotationScanner;
import net.trellisframework.core.application.ApplicationContextProvider;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AutoConfigureOrder
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ImportAutoConfiguration(ApplicationContextProvider.class)
public class RedisCacheConfig {

    private final CacheProperties property;

    public RedisCacheConfig(CacheProperties property) {
        this.property = property;
    }

    @Primary
    @Bean(CacheManagers.REDIS)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        Set<TTL> elements = AnnotationScanner.ttl();
        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
        for (TTL element : elements) {
            RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer())).entryTtl(Duration.ofSeconds(TimeUnit.SECONDS.convert(element.getTtl(), element.getUnit())));
            Optional.ofNullable(property.getRedis()).map(CacheProperties.Redis::getKeyPrefix).ifPresent(configuration::prefixCacheNameWith);
            Optional.ofNullable(property.getRedis()).filter(x -> !x.isCacheNullValues()).ifPresent(x -> configuration.disableCachingNullValues());
            Optional.ofNullable(property.getRedis()).filter(x -> !x.isUseKeyPrefix()).ifPresent(x -> configuration.disableKeyPrefix());
            Arrays.stream(element.getName()).forEach(name -> initialCacheConfigurations.put(name, configuration));
        }
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));
        Optional.ofNullable(property.getRedis()).map(CacheProperties.Redis::getTimeToLive).ifPresent(defaultConfiguration::entryTtl);
        Optional.ofNullable(property.getRedis()).map(CacheProperties.Redis::getKeyPrefix).ifPresent(defaultConfiguration::prefixCacheNameWith);
        Optional.ofNullable(property.getRedis()).filter(x -> !x.isCacheNullValues()).ifPresent(x -> defaultConfiguration.disableCachingNullValues());
        Optional.ofNullable(property.getRedis()).filter(x -> !x.isUseKeyPrefix()).ifPresent(x -> defaultConfiguration.disableKeyPrefix());
        WildcardRedisCacheManager cacheManager = new WildcardRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultConfiguration, initialCacheConfigurations);
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }
}
