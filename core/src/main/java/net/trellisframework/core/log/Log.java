package net.trellisframework.core.log;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    boolean input() default true;
    boolean output() default true;
}