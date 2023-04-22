package net.trellisframework.ui.web.bind;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

    String value() default "";

    String target() default "";

}
