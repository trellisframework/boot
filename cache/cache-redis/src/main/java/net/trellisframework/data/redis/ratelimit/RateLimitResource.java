package net.trellisframework.data.redis.ratelimit;

import lombok.*;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.NotFoundException;

import java.time.Duration;

@RequiredArgsConstructor
public class RateLimitResource<T> implements Payload {
    private final String resourceKey;
    private final String targetKey;
    private final RateLimit resourceLimits;
    private final RateLimit targetLimits;
    @Getter
    private final T resource;
    private volatile String permitId;

    RateLimitResource<T> permit(String permitId) {
        this.permitId = permitId;
        return this;
    }

    public void release() {
        String released = permitId;
        permitId = null;
        AdvancedRateLimiter.releaseResource(resourceKey, permits(resourceLimits), targetKey, permits(targetLimits), released);
    }

    private static RateLimit permits(RateLimit limits) {
        return limits != null && limits.getMaxConcurrent() > 0 ? limits : null;
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
        return AdvancedRateLimiter.canAcquireResource(resourceKey, resourceLimits, targetKey, targetLimits);
    }

    public boolean tryAcquire() {
        String acquired = AdvancedRateLimiter.tryAcquireResource(resourceKey, resourceLimits, targetKey, targetLimits);
        if (acquired == null)
            return false;
        permitId = acquired;
        return true;
    }

    public void acquire() {
        if (!tryAcquire())
            throw new NotFoundException(Messages.NO_AVAILABLE_RESOURCES.getMessage());
    }

}
