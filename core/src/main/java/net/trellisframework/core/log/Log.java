package net.trellisframework.core.log;


import org.apache.logging.log4j.spi.StandardLevel;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String value() default "";
    Option[] options() default {Option.INPUT, Option.OUTPUT};
    When when() default When.ALL;
    StandardLevel level() default StandardLevel.INFO;

    enum Option {
        INPUT,
        OUTPUT,
        EXECUTION_TIME,
        CORRELATION_ID,
        ALL;
    }

    enum When {
        STARTED,
        FINISHED,
        ALL;
    }
}