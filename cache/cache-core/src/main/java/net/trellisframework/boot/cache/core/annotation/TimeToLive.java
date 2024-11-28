package net.trellisframework.boot.cache.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TimeToLive {
    String[] value() default {};

    TimeUnit unit() default TimeUnit.DAYS;

    long ttl() default 1;
}
