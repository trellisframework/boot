package net.trellisframework.core.log;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.spi.StandardLevel;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Logger {
    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(Logger.class);

    private static String getFileName() {
        StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
        return stackTraceElement.length > 3 ? stackTraceElement[3].getFileName() : "";
    }

    private static String getMethodName() {
        StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
        return stackTraceElement.length > 3 ? stackTraceElement[3].getMethodName() : "";
    }

    public static void trace(String message) {
        log(StandardLevel.TRACE, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void trace(String identifier, String message) {
        log(StandardLevel.TRACE, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void trace(String identifier, String message, Throwable t) {
        log(StandardLevel.TRACE, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void trace(String identifier, String message, Object... params) {
        log(StandardLevel.TRACE, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void trace(Runnable runnable, long thresholdMs, String message) {
        trace(runnable, thresholdMs, message, new Object[0]);
    }

    public static void trace(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::trace, message, args);
    }

    public static <T> T trace(String message, Supplier<T> supplier, long thresholdMs) {
        return trace(message, supplier, thresholdMs, new Object[0]);
    }

    public static <T> T trace(String message, Supplier<T> supplier, long thresholdMs, Object... args) {
        return log(supplier, thresholdMs, Logger::trace, message, args);
    }

    public static <T> T trace(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::trace);
    }

    public static void debug(String message) {
        log(StandardLevel.DEBUG, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void debug(String identifier, String message) {
        log(StandardLevel.DEBUG, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void debug(String identifier, String message, Throwable t) {
        log(StandardLevel.DEBUG, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void debug(String identifier, String message, Object... params) {
        log(StandardLevel.DEBUG, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void debug(Runnable runnable, long thresholdMs, String message) {
        debug(runnable, thresholdMs, message, new Object[0]);
    }

    public static void debug(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::debug, message, args);
    }

    public static <T> T debug(Supplier<T> supplier, String message, long thresholdMs) {
        return debug(supplier, thresholdMs, message, new Object[0]);
    }

    public static <T> T debug(Supplier<T> supplier, long thresholdMs, String message, Object... args) {
        return log(supplier, thresholdMs, Logger::debug, message, args);
    }

    public static <T> T debug(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::debug);
    }

    public static void info(String message) {
        log(StandardLevel.INFO, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void info(String identifier, String message) {
        log(StandardLevel.INFO, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void info(String identifier, String message, Throwable t) {
        log(StandardLevel.INFO, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void info(String identifier, String message, Object... params) {
        log(StandardLevel.INFO, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void info(Runnable runnable, String message, long thresholdMs) {
        info(runnable, thresholdMs, message, new Object[0]);
    }

    public static void info(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::info, message, args);
    }

    public static <T> T info(Supplier<T> supplier, String message, long thresholdMs) {
        return info(supplier, thresholdMs, message, new Object[0]);
    }

    public static <T> T info(Supplier<T> supplier, long thresholdMs, String message, Object... args) {
        return log(supplier, thresholdMs, Logger::info, message, args);
    }

    public static <T> T info(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::info);
    }

    public static void warn(String message) {
        log(StandardLevel.WARN, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void warn(String identifier, String message) {
        log(StandardLevel.WARN, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void warn(String identifier, String message, Throwable t) {
        log(StandardLevel.WARN, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void warn(String identifier, String message, Object... params) {
        log(StandardLevel.WARN, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void warn(Runnable runnable, long thresholdMs, String message) {
        warn(runnable, thresholdMs, message, new Object[0]);
    }

    public static void warn(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::warn, message, args);
    }

    public static <T> T warn(Supplier<T> supplier, long thresholdMs, String message) {
        return warn(supplier, thresholdMs, message, new Object[0]);
    }

    public static <T> T warn(Supplier<T> supplier, long thresholdMs, String message, Object... args) {
        return log(supplier, thresholdMs, Logger::warn, message, args);
    }

    public static <T> T warn(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::warn);
    }

    public static void error(String message) {
        log(StandardLevel.ERROR, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void error(String identifier, String message) {
        log(StandardLevel.ERROR, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void error(String identifier, String message, Throwable t) {
        log(StandardLevel.ERROR, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void error(String identifier, String message, Object... params) {
        log(StandardLevel.ERROR, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void error(Runnable runnable, long thresholdMs, String message) {
        error(runnable, thresholdMs, message, new Object[0]);
    }

    public static void error(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::error, message, args);
    }

    public static <T> T error(Supplier<T> supplier, long thresholdMs, String message) {
        return error(supplier, thresholdMs, message, new Object[0]);
    }

    public static <T> T error(Supplier<T> supplier, long thresholdMs, String message, Object... args) {
        return log(supplier, thresholdMs, Logger::error, message, args);
    }

    public static <T> T error(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::error);
    }

    public static void fatal(String message) {
        log(StandardLevel.FATAL, getFileName(), getMethodName(), StringUtils.EMPTY, message, (Throwable) null);
    }

    public static void fatal(String identifier, String message) {
        log(StandardLevel.FATAL, getFileName(), getMethodName(), identifier, message, (Throwable) null);
    }

    public static void fatal(String identifier, String message, Throwable t) {
        log(StandardLevel.FATAL, getFileName(), getMethodName(), identifier, message, t);
    }

    public static void fatal(String identifier, String message, Object... params) {
        log(StandardLevel.FATAL, getFileName(), getMethodName(), identifier, message, params);
    }

    public static void fatal(Runnable runnable, long thresholdMs, String message) {
        fatal(runnable, thresholdMs, message, new Object[0]);
    }

    public static void fatal(Runnable runnable, long thresholdMs, String message, Object... args) {
        log(runnable, thresholdMs, Logger::fatal, message, args);
    }

    public static <T> T fatal(Supplier<T> supplier, long thresholdMs, String message) {
        return fatal(supplier, thresholdMs, message, new Object[0]);
    }

    public static <T> T fatal(Supplier<T> supplier, long thresholdMs, String message, Object... args) {
        return log(supplier, thresholdMs, Logger::fatal, message, args);
    }

    public static <T> T fatal(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message) {
        return log(supplier, condition, message, Logger::fatal);
    }

    private static void log(Runnable runnable, long thresholdMs, Consumer<String> logger, String message, Object[] args) {
        long start = System.currentTimeMillis();
        runnable.run();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > thresholdMs) {
            logger.accept(formatMessage(message, elapsed, args));
        }
    }

    private static <T> T log(Supplier<T> supplier, long thresholdMs, Consumer<String> logger, String message, Object[] args) {
        long start = System.currentTimeMillis();
        T result = supplier.get();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > thresholdMs) {
            logger.accept(formatMessage(message, elapsed, args));
        }
        return result;
    }

    private static <T> T log(Supplier<T> supplier, BiPredicate<Long, T> condition, BiFunction<Long, T, String> message, Consumer<String> logger) {
        long start = System.currentTimeMillis();
        T result = supplier.get();
        long elapsed = System.currentTimeMillis() - start;
        if (condition.test(elapsed, result)) {
            logger.accept(message.apply(elapsed, result));
        }
        return result;
    }

    private static String formatMessage(String format, long elapsed, Object[] args) {
        if (format.contains("%d") || format.contains("%s")) {
            Object[] allArgs = new Object[args.length + 1];
            allArgs[0] = elapsed;
            System.arraycopy(args, 0, allArgs, 1, args.length);
            return String.format(format, allArgs);
        } else {
            if (args.length > 0) {
                return String.format("%s: %d ms", String.format(format, args), elapsed);
            }
            return String.format("%s: %d ms", format, elapsed);
        }
    }

    public static void log(StandardLevel level, String clazz, String method, String identifier, String message, Throwable t) {
        String text = String.format("[%s]-{%s}-(%s)", Thread.currentThread().getName(), clazz, method);
        if (StringUtils.isNotBlank(identifier))
            text = text.concat("-" + identifier);
        text = text.concat("-> " + message);
        if (StandardLevel.TRACE.equals(level))
            logger.trace(text, t);
        else if (StandardLevel.DEBUG.equals(level))
            logger.debug(text, t);
        else if (StandardLevel.INFO.equals(level))
            logger.info(text, t);
        else if (StandardLevel.WARN.equals(level))
            logger.warn(text, t);
        else if (StandardLevel.FATAL.equals(level))
            logger.fatal(text, t);
        else if (StandardLevel.ERROR.equals(level))
            logger.error(text, t);
    }


    public static void log(StandardLevel level, String clazz, String method, String identifier, String message, Object... params) {
        String text = String.format("[%s]-{%s}-(%s)", Thread.currentThread().getName(), clazz, method);
        if (StringUtils.isNotBlank(identifier))
            text = text.concat("-" + identifier);
        text = text.concat("-> " + Optional.ofNullable(params).map(x -> String.format(message, x)).orElse(message));
        if (StandardLevel.TRACE.equals(level))
            logger.trace(text);
        else if (StandardLevel.DEBUG.equals(level))
            logger.debug(text);
        else if (StandardLevel.INFO.equals(level))
            logger.info(text);
        else if (StandardLevel.WARN.equals(level))
            logger.warn(text);
        else if (StandardLevel.FATAL.equals(level))
            logger.fatal(text);
        else if (StandardLevel.ERROR.equals(level))
            logger.error(text);
    }
}
