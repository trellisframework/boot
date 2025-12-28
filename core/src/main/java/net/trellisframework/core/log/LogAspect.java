package net.trellisframework.core.log;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.spi.StandardLevel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

@Aspect
@Component
public class LogAspect {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@within(net.trellisframework.core.log.Log) || @annotation(net.trellisframework.core.log.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        Log log = Optional.ofNullable(method.getAnnotation(Log.class)).orElse(point.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(Log.class));
        Set<Log.Option> opts = new HashSet<>(Arrays.asList(log.options()));
        if (opts.isEmpty()) return point.proceed();
        boolean hasAll = opts.contains(Log.Option.ALL);
        boolean input = hasAll || opts.contains(Log.Option.INPUT);
        boolean output = hasAll || opts.contains(Log.Option.OUTPUT);
        boolean execTime = hasAll || opts.contains(Log.Option.EXECUTION_TIME);
        boolean corrId = hasAll || opts.contains(Log.Option.CORRELATION_ID);
        StandardLevel level = log.level();
        Log.When when = log.when();
        String traceId = corrId ? RandomStringUtils.secure().nextAlphanumeric(8) : "";
        String corrText = corrId ? " CorrelationID= " + traceId : "";
        String inputText = input ? " Args= " + Arrays.toString(point.getArgs()) : "";
        String id = StringUtils.defaultIfBlank(log.value(), "");
        String startCond = log.condition().start();
        String finishCond = log.condition().finish();
        Object result = null;
        String outputText = "";
        StopWatch stopWatch = new StopWatch();
        try {
            if (evaluate(startCond, point.getArgs(), null, null, method, point.getTarget()) && (when == Log.When.STARTED || when == Log.When.ALL))
                Logger.log(level, point.getTarget().getClass().getSimpleName(), method.getName(), id, "Started" + corrText + inputText);
            stopWatch.start();
            result = point.proceed();
            outputText = output ? " Result= " + result : StringUtils.EMPTY;
            return result;
        } catch (Exception e) {
            outputText = output ? " Exception= " + e.getMessage() : StringUtils.EMPTY;
            throw e;
        } finally {
            stopWatch.stop();
            long time = stopWatch.getTotalTimeMillis();
            String timeText = execTime ? " Execution-Time= " + time + "ms" : "";
            if (evaluate(finishCond, point.getArgs(), result, time, method, point.getTarget()) && (when == Log.When.FINISHED || when == Log.When.ALL))
                Logger.log(level, point.getTarget().getClass().getSimpleName(), method.getName(), id, "Finished" + corrText + timeText + inputText + outputText);
        }
    }

    private boolean evaluate(String condition, Object[] args, Object result, Long execTime, Method method, Object target) {
        if (StringUtils.isBlank(condition)) return true;
        try {
            StandardEvaluationContext ctx = createContext(args, result, execTime, method, target);
            return Boolean.TRUE.equals(PARSER.parseExpression(condition).getValue(ctx, Boolean.class));
        } catch (Exception e) {
            return false;
        }
    }

    private StandardEvaluationContext createContext(Object[] args, Object result, Long execTime, Method method, Object target) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        if (args != null && method != null) {
            String[] names = NAME_DISCOVERER.getParameterNames(method);
            if (names != null) {
                IntStream.range(0, Math.min(names.length, args.length)).forEach(i -> ctx.setVariable(names[i], args[i]));
            }
            IntStream.range(0, args.length).forEach(i -> ctx.setVariable("arg" + i, args[i]));
            ctx.setVariable("args", args);
        }
        if (result != null) ctx.setVariable("result", result);
        if (execTime != null) ctx.setVariable("executionTime", execTime);
        if (method != null) ctx.setVariable("methodName", method.getName());
        if (target != null) ctx.setVariable("className", target.getClass().getSimpleName());
        return ctx;
    }
}
