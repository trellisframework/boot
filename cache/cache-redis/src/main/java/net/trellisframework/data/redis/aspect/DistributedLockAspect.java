package net.trellisframework.data.redis.aspect;

import net.trellisframework.data.redis.annotation.DistributedLock;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.RequestTimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.stream.IntStream;

@Aspect
@Component
public class DistributedLockAspect {
    private final RedissonClient client;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();


    public DistributedLockAspect(RedissonClient redis) {
        this.client = redis;
    }

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock lock) throws Throwable {
        String key = getKey(joinPoint, lock.key());
        String lockName = lock.value() + (StringUtils.isNotBlank(key) ? ("::" + key) : StringUtils.EMPTY);
        RLock rLock = lock.fairLock() ? client.getFairLock(lockName) : client.getLock(lockName);
        boolean acquired = rLock.tryLock(lock.waitTime(), lock.leaseTime(), lock.timeUnit());
        if (acquired) {
            try {
                return joinPoint.proceed();
            } finally {
                rLock.unlock();
            }
        }
        throw new RequestTimeoutException(Messages.LOCK_NOT_ACQUIRED.getMessage() + ": " + lockName);
    }

    private String getKey(ProceedingJoinPoint pjp, String key) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        StandardEvaluationContext context = new MethodBasedEvaluationContext(pjp.getTarget(), method, args, nameDiscoverer);
        IntStream.range(0, paramNames.length).forEach(i -> context.setVariable(paramNames[i], args[i]));
        return parser.parseExpression(key).getValue(context, String.class);
    }
}