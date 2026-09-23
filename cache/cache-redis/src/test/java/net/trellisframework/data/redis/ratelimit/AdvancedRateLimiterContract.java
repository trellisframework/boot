package net.trellisframework.data.redis.ratelimit;

import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.http.exception.PreConditionRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour every backend of {@link AdvancedRateLimiter} must satisfy. Subclasses wire either a real Redis
 * ({@link AdvancedRateLimiterRedisTest}) or no Redis at all ({@link AdvancedRateLimiterLocalTest}).
 */
abstract class AdvancedRateLimiterContract {

    /** Prepares the backend for one test. */
    protected abstract void wireBackend();

    static void wire(RedissonClient client) {
        new AdvancedRateLimiter(new ObjectProvider<>() {
            @Override
            public RedissonClient getIfAvailable() {
                return client;
            }
        });
    }

    @BeforeEach
    void resetLimiter() {
        AdvancedRateLimiter.reset();
        wireBackend();
    }

    /** 9. Refreshing a pool's resources must not break acquires that are already in flight. */
    @Test
    void refreshingResourcesDoesNotBreakConcurrentAcquires() throws Exception {
        AdvancedRateLimiter.<String>pool("swap").resources(List.of("r1", "r2", "r3"))
                .resourceLimits(RateLimit.builder().second(60, 10_000_000).build()).build();

        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        AtomicBoolean stop = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(4);
        for (int t = 0; t < 3; t++)
            Thread.ofPlatform().start(() -> {
                try {
                    while (!stop.get()) AdvancedRateLimiter.tryAcquire("swap");
                } catch (Throwable e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        Thread.ofPlatform().start(() -> {
            try {
                while (!stop.get()) AdvancedRateLimiter.setResources("swap", List.of("r1", "r2", "r3"));
            } catch (Throwable e) {
                failures.add(e);
            } finally {
                done.countDown();
            }
        });

        Thread.sleep(1_500);
        stop.set(true);
        done.await();
        assertTrue(failures.isEmpty(), "in-flight acquires must survive a resource refresh, saw: " + failures);
    }

    /** 10. exists() tracks pool registration, and the mutators refuse a pool that was never registered. */
    @Test
    void poolRegistrationIsVisibleAndMutatorsRejectUnknownPools() {
        assertFalse(AdvancedRateLimiter.exists("ghost"));
        assertFalse(AdvancedRateLimiter.containsTargetLimit("ghost", "T"), "an unknown pool holds no target limits");

        RateLimit one = RateLimit.builder().second(1).build();
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.setResources("ghost", List.of("r1")));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.setResourceLimits("ghost", one));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.setTargetLimits("ghost", TargetLimits.create()));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.putTargetLimit("ghost", "T", one));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.putTargetLimitIfAbsent("ghost", "T", one));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.removeTargetLimit("ghost", "T"));
        assertThrows(PreConditionRequiredException.class, () -> AdvancedRateLimiter.tryAcquire("ghost"));

        AdvancedRateLimiter.<String>pool("registered").resources(List.of("r1")).resourceLimits(one).build();
        assertTrue(AdvancedRateLimiter.exists("registered"));
        AdvancedRateLimiter.reset();
        assertFalse(AdvancedRateLimiter.exists("registered"), "reset clears the registry");
    }

    /** 11. setResources swaps which resources the pool draws on; an emptied pool admits nobody. */
    @Test
    void setResourcesReplacesTheResourcesInUse() {
        AdvancedRateLimiter.<String>pool("swapped").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(1).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("swapped"), "r1 has its one permit for this second");
        assertNull(AdvancedRateLimiter.tryAcquire("swapped"), "r1 is spent");

        AdvancedRateLimiter.setResources("swapped", List.of("r2"));
        RateLimitResource<String> resource = AdvancedRateLimiter.tryAcquire("swapped");
        assertNotNull(resource, "r2 keeps its own counter");
        assertEquals("r2", resource.getResource());

        AdvancedRateLimiter.setResources("swapped", List.of());
        assertNull(AdvancedRateLimiter.tryAcquire("swapped"), "an empty pool admits nobody");
    }

    /** 12. setResourceLimits applies to later acquires, against the counters already recorded. */
    @Test
    void setResourceLimitsAppliesToSubsequentAcquires() {
        AdvancedRateLimiter.<String>pool("relimited").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(1).build()).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("relimited"));
        assertNull(AdvancedRateLimiter.tryAcquire("relimited"));

        AdvancedRateLimiter.setResourceLimits("relimited", RateLimit.builder().second(3).build());
        assertNotNull(AdvancedRateLimiter.tryAcquire("relimited"), "one of three used this second");
        assertNotNull(AdvancedRateLimiter.tryAcquire("relimited"));
        assertNull(AdvancedRateLimiter.tryAcquire("relimited"), "the widened limit still binds");
    }

    /** 13. The per-target limit map: put, putIfAbsent, contains and remove, each visible to the next acquire. */
    @Test
    void targetLimitsCanBeManagedAfterThePoolIsBuilt() {
        AdvancedRateLimiter.<String>pool("targets").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10).build()).build();
        assertFalse(AdvancedRateLimiter.containsTargetLimit("targets", "SEND"));

        AdvancedRateLimiter.putTargetLimit("targets", "SEND", RateLimit.builder().second(1).build());
        assertTrue(AdvancedRateLimiter.containsTargetLimit("targets", "SEND"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "SEND"));
        assertNull(AdvancedRateLimiter.tryAcquire("targets", "SEND"), "the target's own limit binds");
        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "READ"), "an unlimited target is unaffected");

        AdvancedRateLimiter.putTargetLimitIfAbsent("targets", "SEND", RateLimit.builder().second(9).build());
        assertNull(AdvancedRateLimiter.tryAcquire("targets", "SEND"), "putIfAbsent must not overwrite the 1/s limit");

        AdvancedRateLimiter.removeTargetLimit("targets", "SEND");
        assertFalse(AdvancedRateLimiter.containsTargetLimit("targets", "SEND"));
        assertNotNull(AdvancedRateLimiter.tryAcquire("targets", "SEND"), "without a target limit only the pool limit applies");

        AdvancedRateLimiter.removeTargetLimit("targets", "SEND");
    }

    /** 14. setTargetLimits replaces the whole map, default limit included. */
    @Test
    void setTargetLimitsReplacesTheWholeMap() {
        AdvancedRateLimiter.<String>pool("retargeted").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10).build())
                .targetLimits(TargetLimits.create().put("SEND", RateLimit.builder().second(1).build())).build();
        assertNotNull(AdvancedRateLimiter.tryAcquire("retargeted", "SEND"));
        assertNull(AdvancedRateLimiter.tryAcquire("retargeted", "SEND"));

        AdvancedRateLimiter.setTargetLimits("retargeted", TargetLimits.create()
                .defaultLimit(RateLimit.builder().second(5).build()));
        assertFalse(AdvancedRateLimiter.containsTargetLimit("retargeted", "SEND"), "the old map is gone");
        assertNotNull(AdvancedRateLimiter.tryAcquire("retargeted", "SEND"), "the default limit now applies");
        assertNotNull(AdvancedRateLimiter.tryAcquire("retargeted", "ANY"), "and to every other target too");
    }

    /** 15. acquire() is tryAcquire() that raises instead of returning nothing. */
    @Test
    void acquireThrowsWhenNothingIsAvailable() {
        AdvancedRateLimiter.<String>pool("strict").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(10).maxConcurrent(1, Duration.ofSeconds(10)).build()).build();

        RateLimitResource<String> resource = AdvancedRateLimiter.acquire("strict");
        assertNotNull(resource);
        assertThrows(NotFoundException.class, () -> AdvancedRateLimiter.acquire("strict"), "the only permit is held");
        assertThrows(NotFoundException.class, resource::acquire, "the resource handle raises the same way");

        resource.release();
        assertTrue(resource.tryAcquire(), "the freed permit is available again through the handle");
    }

    /** 16. release() hands back a concurrency permit; it does not refund the window's allowance. */
    @Test
    void releaseFreesThePermitButNotTheWindowAllowance() {
        AdvancedRateLimiter.<String>pool("spent").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(1).maxConcurrent(1, Duration.ofSeconds(10)).build()).build();

        RateLimitResource<String> resource = AdvancedRateLimiter.acquire("spent");
        resource.release();
        assertNull(AdvancedRateLimiter.tryAcquire("spent"), "the one request allowed this second is already used");
    }

    /** 17. A caller whose permit already expired must not free the permit that replaced it. */
    @Test
    void releaseAfterExpiryDoesNotStealAnotherCallersPermit() throws Exception {
        AdvancedRateLimiter.<String>pool("slow").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(99).maxConcurrent(1, Duration.ofMillis(120)).build()).build();

        RateLimitResource<String> slow = AdvancedRateLimiter.acquire("slow");
        Thread.sleep(200);
        assertNotNull(AdvancedRateLimiter.tryAcquire("slow"), "the timed-out permit frees the slot");

        slow.release();
        assertNull(AdvancedRateLimiter.tryAcquire("slow"), "the permit taken meanwhile must still hold the slot");
    }

    /** 18. Releasing twice hands back one permit, not two. */
    @Test
    void releasingTwiceFreesOnlyOnePermit() {
        AdvancedRateLimiter.<String>pool("twice").resources(List.of("r1"))
                .resourceLimits(RateLimit.builder().second(99).maxConcurrent(1, Duration.ofSeconds(10)).build()).build();

        RateLimitResource<String> first = AdvancedRateLimiter.acquire("twice");
        first.release();
        assertNotNull(AdvancedRateLimiter.tryAcquire("twice"), "the slot is free again");

        first.release();
        assertNull(AdvancedRateLimiter.tryAcquire("twice"), "the second release must free nothing");
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
