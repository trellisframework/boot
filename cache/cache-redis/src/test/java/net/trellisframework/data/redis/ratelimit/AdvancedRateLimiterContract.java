package net.trellisframework.data.redis.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour every backend of {@link AdvancedRateLimiter} must satisfy. Subclasses wire either a real Redis
 * ({@link AdvancedRateLimiterRedisTest}) or no Redis at all ({@link AdvancedRateLimiterLocalTest}).
 */
abstract class AdvancedRateLimiterContract {

    /** Prepares the backend and {@code ApplicationContextProvider.context} for one test. */
    protected abstract void wireBackend();

    @BeforeEach
    void resetLimiter() {
        AdvancedRateLimiter.reset();
        wireBackend();
    }

    @AfterEach
    void clearLimiter() {
        AdvancedRateLimiter.reset();
    }

    /** 1. Fixed window: 5/1s → five acquires succeed, the sixth fails, and it succeeds again once the window rolls. */
    @Test
    void fixedWindowLimitsRequestsPerWindow() {
        AdvancedRateLimiter.<String>pool("fixed").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(5).build()).build();

        for (int i = 0; i < 5; i++)
            assertNotNull(AdvancedRateLimiter.tryAcquire("fixed"), "acquire #" + (i + 1) + " should succeed");
        assertNull(AdvancedRateLimiter.tryAcquire("fixed"), "6th acquire in the same window must fail");
        assertFalse(AdvancedRateLimiter.canAcquire("fixed"));

        assertTrue(waitUntil(() -> AdvancedRateLimiter.canAcquire("fixed"), Duration.ofSeconds(3)), "window never rolled");
        assertNotNull(AdvancedRateLimiter.tryAcquire("fixed"), "acquire after the window rolled must succeed");
    }

    /** 2. maxConcurrent=2 with permitTimeout=100ms → two acquire, the third fails, and after 100ms without release it succeeds. */
    @Test
    void concurrentPermitsExpireAfterPermitTimeout() {
        AdvancedRateLimiter.<String>pool("permits").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().maxConcurrent(2, Duration.ofMillis(100)).build()).build();

        assertNotNull(AdvancedRateLimiter.tryAcquire("permits"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("permits"));
        assertNull(AdvancedRateLimiter.tryAcquire("permits"), "3rd concurrent acquire must fail");

        assertTrue(waitUntil(() -> AdvancedRateLimiter.canAcquire("permits"), Duration.ofSeconds(2)), "permits never expired");
        assertNotNull(AdvancedRateLimiter.tryAcquire("permits"), "acquire after permit timeout must succeed");
    }

    /** 3a. release() frees a permit immediately. */
    @Test
    void releaseFreesPermit() {
        AdvancedRateLimiter.<String>pool("release").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().maxConcurrent(1, Duration.ofMinutes(1)).build()).build();

        RateLimitResource<String> held = AdvancedRateLimiter.tryAcquire("release");
        assertNotNull(held);
        assertNull(AdvancedRateLimiter.tryAcquire("release"), "permit is held");
        held.release();
        assertNotNull(AdvancedRateLimiter.tryAcquire("release"), "released permit must be reusable");
    }

    /** 3b. coolOff(d) blocks acquires for d; the first successful acquire after that clears the cool-off. */
    @Test
    void coolOffBlocksUntilExpiryAndIsClearedByNextAcquire() {
        AdvancedRateLimiter.<String>pool("cooloff").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(100).build()).build();

        RateLimitResource<String> held = AdvancedRateLimiter.tryAcquire("cooloff");
        assertNotNull(held);
        held.coolOff(Duration.ofMillis(300));
        assertNull(AdvancedRateLimiter.tryAcquire("cooloff"), "must be blocked during cool-off");
        assertFalse(AdvancedRateLimiter.canAcquire("cooloff"));

        assertTrue(waitUntil(() -> AdvancedRateLimiter.canAcquire("cooloff"), Duration.ofSeconds(2)), "cool-off never expired");
        assertNotNull(AdvancedRateLimiter.tryAcquire("cooloff"), "acquire after cool-off must succeed");

        // a long cool-off applied then cleared by a successful acquire must not resurface
        held.coolOff(Duration.ofMillis(200));
        assertTrue(waitUntil(() -> AdvancedRateLimiter.tryAcquire("cooloff") != null, Duration.ofSeconds(2)));
        assertNotNull(AdvancedRateLimiter.tryAcquire("cooloff"), "cool-off must stay cleared after a successful acquire");
    }

    /** 4. Round-robin over 3 resources where resource 1 is exhausted → returns resource 2, then resource 3. */
    @Test
    void roundRobinSkipsExhaustedResource() {
        AdvancedRateLimiter.<String>pool("rr").resources(List.of("r1", "r2", "r3"))
                .resourceLimits(RateLimit.builder().second(1).build()).build();

        RateLimitResource<String> first = AdvancedRateLimiter.tryAcquire("rr");
        assertNotNull(first);
        assertEquals("r1", first.getResource());
        RateLimitResource<String> second = AdvancedRateLimiter.tryAcquire("rr");
        assertNotNull(second);
        assertEquals("r2", second.getResource());
        RateLimitResource<String> third = AdvancedRateLimiter.tryAcquire("rr");
        assertNotNull(third);
        assertEquals("r3", third.getResource());
        assertNull(AdvancedRateLimiter.tryAcquire("rr"), "all three resources are exhausted");
    }

    @Test
    void roundRobinStartsAtNextResourceWhenPreviousIsExhausted() {
        RateLimit limits = RateLimit.builder().second(1).build();
        AdvancedRateLimiter.<String>pool("rr2").resources(List.of("r1", "r2", "r3")).resourceLimits(limits).build();

        // exhaust r1 through a resource handle so the pool's round-robin pointer still points at r1
        assertTrue(new RateLimitResource<>("rate-limiter:rr2:r1", null, limits, null, "r1").tryAcquire());
        assertEquals("r2", AdvancedRateLimiter.tryAcquire("rr2").getResource(), "r1 exhausted → r2");
        assertEquals("r3", AdvancedRateLimiter.tryAcquire("rr2").getResource(), "pointer at r2, exhausted → r3");
    }

    /** Target limits are enforced per resource+target and all-or-nothing with the resource limit. */
    @Test
    void targetLimitsAreIndependentPerTargetAndAtomicWithResourceLimit() {
        AdvancedRateLimiter.<String>pool("targets").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(3).build())
                .targetLimits(TargetLimits.create().put("A", RateLimit.builder().second(1).build())
                        .put("B", RateLimit.builder().second(5).build()))
                .build();

        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "A"));
        assertNull(AdvancedRateLimiter.tryAcquire("targets", "A"), "target A allows 1/s");
        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "B"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "B"));
        // resource limit (3/s) is now exhausted although target B still has room; the failed A attempt must
        // not have consumed a resource slot
        assertNull(AdvancedRateLimiter.tryAcquire("targets", "B"), "resource limit must cap all targets");
    }

    /** Fox flow: pool built without limits, limit installed via putRateLimit after the first acquire. */
    @Test
    void putRateLimitOverrideAppliesToSubsequentAcquires() {
        AdvancedRateLimiter.<String>pool("fox").resources(List.of("account")).build();

        RateLimitResource<String> first = AdvancedRateLimiter.tryAcquire("fox", "SEARCH");
        assertNotNull(first, "no limits configured → first acquire is free");
        first.putRateLimit(RateLimit.builder().second(1).maxConcurrent(1, Duration.ofSeconds(5)).build());

        RateLimitResource<String> second = AdvancedRateLimiter.tryAcquire("fox", "SEARCH");
        assertNotNull(second, "first limited acquire");
        assertNull(AdvancedRateLimiter.tryAcquire("fox", "SEARCH"), "override 1/s must now be enforced");
        assertNotNull(AdvancedRateLimiter.tryAcquire("fox", "OTHER"), "override is per target");
    }

    static boolean waitUntil(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }
}
