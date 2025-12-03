package net.trellisframework.data.redis.ratelimit;

import java.util.HashMap;
import java.util.Map;

public class TargetLimits {
    private final Map<String, RateLimit> limits = new HashMap<>();
    private RateLimit defaultLimit;

    public static TargetLimits create() {
        return new TargetLimits();
    }

    public TargetLimits add(String target, RateLimit rateLimit) {
        limits.put(target, rateLimit);
        return this;
    }

    public TargetLimits defaultLimit(RateLimit rateLimit) {
        this.defaultLimit = rateLimit;
        return this;
    }

    public RateLimit get(String target) {
        return limits.getOrDefault(target, defaultLimit);
    }
}

