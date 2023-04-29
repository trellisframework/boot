package net.trellisframework.core.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.text.MessageFormat;
import java.util.Arrays;

@Aspect
@Component
public class LogAspect {

    @Around("@within(Log) || @annotation(Log)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        try {
            System.out.println(MessageFormat.format("Enter: {0}.{1}() with argument[s] = {2}", point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), Arrays.toString(point.getArgs())));
            stopWatch.start();
            Object result = point.proceed();
            stopWatch.stop();
            System.out.println(MessageFormat.format("Exit: {0}.{1}() execution-time: {2}ms with result = {3}", point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), stopWatch.getTotalTimeMillis(), result));
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            System.out.println(MessageFormat.format("Exit: {0}.{1}() execution-time: {2}ms with error = {3}", point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), stopWatch.getTotalTimeMillis(), e.getMessage()));
            throw e;
        }
    }

}