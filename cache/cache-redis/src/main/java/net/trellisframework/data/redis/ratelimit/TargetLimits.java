package net.trellisframework.data.redis.ratelimit;

import net.trellisframework.core.payload.Payload;

import java.util.HashMap;
import java.util.Map;

public class TargetLimits implements Payload {
    private final Map<String, RateLimit> limits = new HashMap<>();
    private RateLimit defaultLimit;

    public static TargetLimits create() {
        return new TargetLimits();
    }

    public TargetLimits putAll(Map<String, RateLimit> rateLimits) {
        if (rateLimits != null) {
            limits.putAll(rateLimits);
        }
        return this;
    }

    public TargetLimits put(String target, RateLimit rateLimit) {
        limits.put(target, rateLimit);
        return this;
    }

    public TargetLimits putIfAbsent(String target, RateLimit rateLimit) {
        limits.putIfAbsent(target, rateLimit);
        return this;
    }

    public TargetLimits remove(String target) {
        limits.remove(target);
        return this;
    }

    public boolean contains(String target) {
        return limits.containsKey(target);
    }

    public TargetLimits defaultLimit(RateLimit rateLimit) {
        this.defaultLimit = rateLimit;
        return this;
    }

    public RateLimit get(String target) {
        return limits.getOrDefault(target, defaultLimit);
    }
}
