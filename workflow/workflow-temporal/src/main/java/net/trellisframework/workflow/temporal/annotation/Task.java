package net.trellisframework.workflow.temporal.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Task {

    Retry retry() default @Retry;

    String timeout() default DEFAULT_TIMEOUT;

    String heartbeat() default DEFAULT_HEARTBEAT;

    String DEFAULT_TIMEOUT = "1h";
    String DEFAULT_HEARTBEAT = "10s";
}
