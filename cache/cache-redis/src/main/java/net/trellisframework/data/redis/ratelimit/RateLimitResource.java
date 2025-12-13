package net.trellisframework.data.redis.ratelimit;

import lombok.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RateLimitResource<T> {
    private final String resourceKey;
    private final String targetKey;
    private final RateLimit resourceLimits;
    private final RateLimit targetLimits;
    @Getter
    private final T resource;

    public void release() {
        if (resourceLimits != null && resourceLimits.getMaxConcurrent() > 0)
            AdvancedRateLimiter.releaseResource(resourceKey, resourceLimits);
        if (targetLimits != null && targetLimits.getMaxConcurrent() > 0)
            AdvancedRateLimiter.releaseResource(targetKey, targetLimits);
    }

    public void coolOff() {
        if (resourceLimits != null)
            AdvancedRateLimiter.applyCoolOff(resourceKey, resourceLimits.defaultCoolOff, resourceLimits);
        if (targetLimits != null)
            AdvancedRateLimiter.applyCoolOff(targetKey, targetLimits.defaultCoolOff, targetLimits);
    }

    public void coolOff(Duration duration) {
        if (resourceLimits != null)
            AdvancedRateLimiter.applyCoolOff(resourceKey, duration != null ? duration : resourceLimits.defaultCoolOff, resourceLimits);
        if (targetLimits != null)
            AdvancedRateLimiter.applyCoolOff(targetKey, duration != null ? duration : targetLimits.defaultCoolOff, targetLimits);
    }

    public void putRateLimit(RateLimit rateLimit) {
        if (targetKey != null)
            AdvancedRateLimiter.setRateLimitOverride(targetKey, rateLimit);
        else
            AdvancedRateLimiter.setRateLimitOverride(resourceKey, rateLimit);
    }
}
