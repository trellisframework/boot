package net.trellisframework.data.redis.ratelimit;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 7. No-Redis mode: without a {@link RedissonClient} bean the contract holds on the in-JVM fallback. */
class AdvancedRateLimiterLocalTest extends AdvancedRateLimiterContract {

    @Override
    protected void wireBackend() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(RedissonClient.class)).thenThrow(new NoSuchBeanDefinitionException(RedissonClient.class));
        ApplicationContextProvider.context = ctx;
    }
}
