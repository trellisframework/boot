package net.trellisframework.workflow.temporal.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    
    int maxAttempts() default 1;
    
    Backoff backoff() default @Backoff;
    
    Class<? extends Throwable>[] include() default {};
    
    Class<? extends Throwable>[] exclude() default {};
    
}
