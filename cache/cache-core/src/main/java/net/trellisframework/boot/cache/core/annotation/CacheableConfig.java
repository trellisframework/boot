package net.trellisframework.boot.cache.core.annotation;

import net.trellisframework.boot.cache.core.constant.CacheSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CacheableConfig {
    String[] value() default {};

    String ttl() default "";

    CacheSerializer serializer() default CacheSerializer.JDK;

}
