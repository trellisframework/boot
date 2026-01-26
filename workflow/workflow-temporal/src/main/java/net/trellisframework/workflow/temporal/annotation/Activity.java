package net.trellisframework.workflow.temporal.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Activity {

    Retry retry() default @Retry;

    String startToCloseTimeout() default DEFAULT_START_TO_CLOSE_TIMEOUT;

    String scheduleToStartTimeout() default "";

    String scheduleToCloseTimeout() default "";

    String heartbeat() default DEFAULT_HEARTBEAT;

    boolean logStackTrace() default false;

    String DEFAULT_START_TO_CLOSE_TIMEOUT = "60s";
    String DEFAULT_HEARTBEAT = "10s";
}
