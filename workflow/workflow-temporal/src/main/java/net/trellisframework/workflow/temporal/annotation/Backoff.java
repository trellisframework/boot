package net.trellisframework.workflow.temporal.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Backoff {

    long delay() default 1000;

    long maxDelay() default 60000;

    double multiplier() default 2.0;

}

