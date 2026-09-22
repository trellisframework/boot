package net.trellisframework.data.redis.ratelimit;

/** 7. No-Redis mode: without a {@link RedissonClient} bean the contract holds on the in-JVM fallback. */
class AdvancedRateLimiterLocalTest extends AdvancedRateLimiterContract {

    @Override
    protected void wireBackend() {
        wire(null);
    }
}
