package net.trellisframework.data.redis.aspect;

import net.trellisframework.data.redis.annotation.Lock;
import net.trellisframework.data.redis.constant.Messages;
import net.trellisframework.http.exception.RequestTimeoutException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class LockAspect {
    private final RedissonClient client;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();


    public LockAspect(RedissonClient redis) {
        this.client = redis;
    }

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint joinPoint, Lock lock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String expressionKey = parseKey(joinPoint, method, lock.key());
        String lockName = lock.value() + "::" + expressionKey;
        RLock rLock = client.getLock(lockName);
        boolean acquired = rLock.tryLock(lock.waitTime(), lock.leaseTime(), lock.timeUnit());
        if (acquired) {
            try {
                return joinPoint.proceed();
            } finally {
                rLock.unlock();
            }
        }
        throw new RequestTimeoutException(Messages.LOCK_NOT_ACQUIRED);
    }

    private String parseKey(ProceedingJoinPoint pjp, Method method, String keyExpression) {
        Object[] args = pjp.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        StandardEvaluationContext context = new MethodBasedEvaluationContext(pjp.getTarget(), method, args, nameDiscoverer);
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}