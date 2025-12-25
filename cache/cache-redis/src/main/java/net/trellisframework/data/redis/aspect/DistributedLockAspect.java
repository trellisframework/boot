package net.trellisframework.data.redis.aspect;

import net.trellisframework.data.redis.annotation.DistributedLock;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.RequestTimeoutException;
import net.trellisframework.util.string.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
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
        if (StringUtils.isNotBlank(lock.cooldown())) {
            return handleCooldown(joinPoint, lock);
        }
        String key = Optional.ofNullable(StringUtil.nullIfBlank(lock.key())).map(x -> getKey(joinPoint, x)).orElse(null);
        String lockName = lock.value() + (StringUtils.isNotBlank(key) ? ("::" + key) : StringUtils.EMPTY);
        RLock rLock = lock.fairLock() ? client.getFairLock(lockName) : client.getLock(lockName);
        long waitTimeMillis = !lock.skipIfLocked() && StringUtils.isNotBlank(lock.waitTime()) ? DurationStyle.SIMPLE.parse(lock.waitTime()).toMillis() : 0;
        long leaseTimeMillis  = StringUtils.isNoneBlank(lock.leaseTime()) ? DurationStyle.SIMPLE.parse(lock.leaseTime()).toMillis() : -1;
        boolean acquired = rLock.tryLock(waitTimeMillis, leaseTimeMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (acquired) {
            try {
                return joinPoint.proceed();
            } finally {
                rLock.unlock();
            }
        }
        if (lock.skipIfLocked()) {
            return null;
        }
        throw new RequestTimeoutException(Messages.LOCK_NOT_ACQUIRED.getMessage() + ": " + lockName);
    }

    private Object handleCooldown(ProceedingJoinPoint joinPoint, DistributedLock lock) throws Throwable {
        String key = Optional.ofNullable(lock.key()).filter(StringUtils::isNotBlank).map(x -> getKey(joinPoint, x)).orElseGet(() -> getDefaultKey(joinPoint));
        String redisKey = "cooldown:" + lock.value() + (StringUtils.isNotBlank(key) ? ("::" + key) : StringUtils.EMPTY);
        RBucket<String> bucket = client.getBucket(redisKey, StringCodec.INSTANCE);
        Long lastSuccessTime = Optional.ofNullable(bucket.get()).map(Long::parseLong).orElse(null);
        long currentTime = System.currentTimeMillis();
        Duration duration = DurationStyle.SIMPLE.parse(lock.cooldown());
        long intervalMillis = duration.toMillis();
        if (lastSuccessTime != null && (currentTime - lastSuccessTime) < intervalMillis) {
            return null;
        }
        Object result = joinPoint.proceed();
        long ttl = intervalMillis + 1000;
        bucket.set(String.valueOf(currentTime), Duration.ofMillis(ttl));
        return result;
    }

    private String getDefaultKey(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String className = pjp.getTarget().getClass().getName();
        String methodName = method.getName();
        return className + "::" + methodName;
    }

    private String getKey(ProceedingJoinPoint pjp, String key) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        StandardEvaluationContext context = new MethodBasedEvaluationContext(pjp.getTarget(), method, args, nameDiscoverer);
        if (paramNames != null) {
            IntStream.range(0, paramNames.length).forEach(i -> context.setVariable(paramNames[i], args[i]));
        }
        return parser.parseExpression(key).getValue(context, String.class);
    }
}