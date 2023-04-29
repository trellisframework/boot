package net.trellisframework.util.benchmark;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class BenchmarkAspect {

    @Around("@annotation(Benchmark)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        String className = point.getSignature().getDeclaringTypeName();
        String methodName = point.getSignature().getName();
        StopWatch stopWatch = new StopWatch();
        System.out.println(className + "->" + methodName + " start");
        stopWatch.start();
        Object result = point.proceed();
        stopWatch.stop();
        System.out.println(className + "->" + methodName + " execution time: " + stopWatch.getTotalTimeMillis() + "ms");
        return result;
    }

}