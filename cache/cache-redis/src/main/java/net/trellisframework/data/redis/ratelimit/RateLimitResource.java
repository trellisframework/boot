package net.trellisframework.data.redis.ratelimit;

import lombok.*;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.NotFoundException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RateLimitResource<T> implements Payload {
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

    public boolean canAcquire() {
        if (resourceLimits != null && !AdvancedRateLimiter.canAcquireResource(resourceKey, resourceLimits))
            return false;
        return targetLimits == null || AdvancedRateLimiter.canAcquireResource(targetKey, targetLimits);
    }

    public boolean tryAcquire() {
        if (resourceLimits != null && !AdvancedRateLimiter.tryAcquireResource(resourceKey, resourceLimits))
            return false;
        return targetLimits == null || AdvancedRateLimiter.tryAcquireResource(targetKey, targetLimits);
    }

    public void acquire() {
        if (!tryAcquire())
            throw new NotFoundException(Messages.NO_AVAILABLE_RESOURCES.getMessage());
    }

}
