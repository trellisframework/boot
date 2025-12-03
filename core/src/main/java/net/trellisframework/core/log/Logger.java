package net.trellisframework.core.log;


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.spi.StandardLevel;

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


    public static void log(StandardLevel level, String clazz, String method, String identifier, String message, Throwable t) {
        String text = String.format("[%s]-{%s}-(%s)", Thread.currentThread().getName(), clazz, method);
        if (StringUtils.isNotBlank(identifier))
            text = text.concat("-" + identifier);
        text = text.concat("->" + message);
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
        text = text.concat("->" + message);
        if (StandardLevel.TRACE.equals(level))
            logger.trace(text, params);
        else if (StandardLevel.DEBUG.equals(level))
            logger.debug(text, params);
        else if (StandardLevel.INFO.equals(level))
            logger.info(text, params);
        else if (StandardLevel.WARN.equals(level))
            logger.warn(text, params);
        else if (StandardLevel.FATAL.equals(level))
            logger.fatal(text, params);
        else if (StandardLevel.ERROR.equals(level))
            logger.error(text, params);
    }
}
