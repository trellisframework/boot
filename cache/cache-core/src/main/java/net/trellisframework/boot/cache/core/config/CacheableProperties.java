package net.trellisframework.boot.cache.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
@ConfigurationProperties("spring.cache")
public class CacheableProperties {

    /**
     * Random spread applied to every entry's time to live, as a fraction of the configured TTL, so entries written together do not expire together. 0 disables it.
     */
    private double ttlJitter = 0.1;

    public Duration jitter(Duration ttl) {
        long spread = Math.round(ttl.toMillis() * ttlJitter);
        return spread <= 0 ? ttl : ttl.plusMillis(ThreadLocalRandom.current().nextLong(-spread, spread + 1));
    }
}
