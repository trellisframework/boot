package net.trellisframework.data.redis.config;

import net.trellisframework.boot.cache.core.config.CacheableProperties;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@AutoConfigureOrder
@Configuration
@EnableConfigurationProperties({CacheProperties.class, CacheableProperties.class})
@ImportAutoConfiguration(ApplicationContextProvider.class)
public class RedisCacheConfig {

    private final CacheProperties property;
    private final CacheableProperties ttlProperties;

    public RedisCacheConfig(CacheProperties property, CacheableProperties ttlProperties) {
        this.property = property;
        this.ttlProperties = ttlProperties;
    }

    @Primary
    @Bean(CacheManagers.REDIS)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        for (CacheableConfig element : AnnotationScanner.cacheableConfig()) {
            RedisCacheConfiguration configuration = configure(RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(serializer(element.getSerializer())), element.getTtl());
            Arrays.stream(element.getName()).forEach(name -> configurations.put(name, configuration));
        }
        RedisCacheConfiguration defaults = configure(RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer())), property.getRedis().getTimeToLive());
        WildcardRedisCacheManager cacheManager = new WildcardRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaults, configurations);
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }

    RedisCacheConfiguration configure(RedisCacheConfiguration configuration, Duration ttl) {
        CacheProperties.Redis redis = property.getRedis();
        if (ttl != null)
            configuration = configuration.entryTtl((key, value) -> ttlProperties.jitter(ttl));
        if (redis.getKeyPrefix() != null)
            configuration = configuration.prefixCacheNameWith(redis.getKeyPrefix());
        if (!redis.isCacheNullValues())
            configuration = configuration.disableCachingNullValues();
        if (!redis.isUseKeyPrefix())
            configuration = configuration.disableKeyPrefix();
        return configuration;
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
