package net.trellisframework.workflow.temporal.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Workflow {

    String taskQueue() default "";

    String executionTimeout() default "";

    String runTimeout() default "";

    String taskTimeout() default "";

    String version() default DEFAULT_VERSION;

    String DEFAULT_VERSION = "0.0.0";

}
