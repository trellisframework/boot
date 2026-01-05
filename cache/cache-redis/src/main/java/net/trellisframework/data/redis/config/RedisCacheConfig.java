package net.trellisframework.data.redis.config;

import net.trellisframework.boot.cache.core.constant.CacheManagers;
import net.trellisframework.boot.cache.core.constant.CacheSerializer;
import net.trellisframework.boot.cache.core.payload.CacheableConfig;
import net.trellisframework.boot.cache.core.scanner.AnnotationScanner;
import net.trellisframework.core.application.ApplicationContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.*;

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
        Set<CacheableConfig> elements = AnnotationScanner.cacheableConfig();
        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
        for (CacheableConfig element : elements) {
            RedisCacheConfiguration configuration = element.getTtl() == null ? RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializer(element.getSerializer())) : RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializer(element.getSerializer())).entryTtl(element.getTtl());
            Optional.of(property.getRedis()).map(CacheProperties.Redis::getKeyPrefix).ifPresent(configuration::prefixCacheNameWith);
            Optional.of(property.getRedis()).filter(x -> !x.isCacheNullValues()).ifPresent(x -> configuration.disableCachingNullValues());
            Optional.of(property.getRedis()).filter(x -> !x.isUseKeyPrefix()).ifPresent(x -> configuration.disableKeyPrefix());
            Arrays.stream(element.getName()).forEach(name -> initialCacheConfigurations.put(name, configuration));
        }
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig().serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string())).serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));
        Optional.of(property.getRedis()).map(CacheProperties.Redis::getTimeToLive).ifPresent(defaultConfiguration::entryTtl);
        Optional.of(property.getRedis()).map(CacheProperties.Redis::getKeyPrefix).ifPresent(defaultConfiguration::prefixCacheNameWith);
        Optional.of(property.getRedis()).filter(x -> !x.isCacheNullValues()).ifPresent(x -> defaultConfiguration.disableCachingNullValues());
        Optional.of(property.getRedis()).filter(x -> !x.isUseKeyPrefix()).ifPresent(x -> defaultConfiguration.disableKeyPrefix());
        WildcardRedisCacheManager cacheManager = new WildcardRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultConfiguration, initialCacheConfigurations);
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }

    private RedisSerializationContext.SerializationPair<?> serializer(CacheSerializer serializer) {
        return switch (serializer) {
            case JSON -> RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json());
            case BYTE_ARRAY -> RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.byteArray());
            case STRING -> RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string());
            default -> RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.java());
        };

    }

    @Bean
    public RedissonClient redissonClient(DataRedisProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(StringUtils.defaultIfBlank(properties.getUrl(), "redis://" + properties.getHost() + ":" + properties.getPort()))
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword());
        return Redisson.create(config);
    }
}
