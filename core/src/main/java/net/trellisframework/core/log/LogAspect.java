package net.trellisframework.core.log;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.spi.StandardLevel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Aspect
@Component
public class LogAspect {

    @Around("@within(net.trellisframework.core.log.Log) || @annotation(net.trellisframework.core.log.Log)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        Log annotation = Optional.ofNullable(method.getAnnotation(Log.class)).orElse(point.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(Log.class));
        List<Log.Option> options = Optional.ofNullable(annotation.options()).map(List::of).orElse(new ArrayList<>());
        if (ObjectUtils.isEmpty(options))
            return point.proceed();

        boolean input = options.stream().anyMatch(x -> Log.Option.ALL.equals(x) || Log.Option.INPUT.equals(x));
        boolean output = options.stream().anyMatch(x -> Log.Option.ALL.equals(x) || Log.Option.OUTPUT.equals(x));
        boolean executionTime = options.stream().anyMatch(x -> Log.Option.ALL.equals(x) || Log.Option.EXECUTION_TIME.equals(x));
        boolean correlationId = options.stream().anyMatch(x -> Log.Option.ALL.equals(x) || Log.Option.CORRELATION_ID.equals(x));
        StandardLevel level = Optional.ofNullable(annotation.level()).orElse(StandardLevel.INFO);
        Log.When when = Optional.ofNullable(annotation.when()).orElse(Log.When.ALL);
        String traceId = RandomStringUtils.randomAlphanumeric(8);
        StopWatch stopWatch = new StopWatch();
        String correlationIdText = correlationId ? " CorrelationID= " + traceId : StringUtils.EMPTY;
        String inputText = input ? " Args= " + Arrays.toString(point.getArgs()) : StringUtils.EMPTY;
        String outputText = StringUtils.EMPTY;
        String identifier = StringUtils.defaultIfBlank(annotation.value(), StringUtils.EMPTY);
        try {
            if (Log.When.STARTED.equals(when) || Log.When.ALL.equals(when))
                Logger.log(level, point.getTarget().getClass().getSimpleName(), method.getName(), identifier, "Started" + correlationIdText + inputText);
            stopWatch.start();
            Object result = point.proceed();
            outputText = output ? " Result= " + result : StringUtils.EMPTY;
            return result;
        } catch (Exception e) {
            outputText = output ? " Exception= " + e.getMessage() : StringUtils.EMPTY;
            throw e;
        } finally {
            stopWatch.stop();
            String executionTimeText = executionTime ? " Execution-Time= " + stopWatch.getTotalTimeMillis() + "ms" : StringUtils.EMPTY;
            if (Log.When.FINISHED.equals(when) || Log.When.ALL.equals(when))
                Logger.log(level, point.getTarget().getClass().getSimpleName(), method.getName(), identifier, "Finished" + correlationIdText + executionTimeText + inputText + outputText);
        }
    }

}