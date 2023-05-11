package net.trellisframework.core.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
public class LogAspect {

    @Around("@within(Log) || @annotation(Log)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Log annotation = method.getAnnotation(Log.class);
        if (annotation == null) {
            method = point.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes());
            annotation = method.getAnnotation(Log.class);
        }
        boolean input = Optional.ofNullable(annotation).map(Log::input).orElse(true);
        boolean output = Optional.ofNullable(annotation).map(Log::output).orElse(true);
        StopWatch stopWatch = new StopWatch();
        String traceId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);
        try {
            System.out.println(MessageFormat.format("Enter: TraceID: {0} Method: {1}.{2}())" + (input ? " -> Argument[s] = {3}" : ""), traceId, point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), Arrays.toString(point.getArgs())));
            stopWatch.start();
            Object result = point.proceed();
            stopWatch.stop();
            System.out.println(MessageFormat.format("Exit: TraceID: {0} Time: {1}ms Method: {2}.{3}()" + (output ? " -> result = {4}" : ""), traceId, stopWatch.getTotalTimeMillis(), point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), result));
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            System.out.println(MessageFormat.format("TraceID: {0} Time: {1}ms Exit: {2}.{3}()" + (output ? " -> Error = {4}" : ""), traceId, stopWatch.getTotalTimeMillis(), point.getSignature().getDeclaringTypeName(), point.getSignature().getName(), e.getMessage()));
            throw e;
        }
    }

}