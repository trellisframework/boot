package net.trellisframework.data.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String value();
    String key() default "";
    boolean fairLock() default false;
    String waitTime() default "60s";
    String leaseTime() default "60s";
    boolean skipIfLocked() default false;
    String cooldown() default "";
}