package net.trellisframework.data.redis.annotation;

import org.springframework.aot.hint.annotation.Reflective;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Reflective
public @interface DistributedLock {
    String value() default "";
    String key() default "";
    long waitTime() default 60;
    long leaseTime() default 60;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}