package net.trellisframework.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

    String name();

    String description() default "";

    String descriptionFile() default "";

    String inputSchemaFile() default "";

    boolean readOnly() default false;

    boolean destructive() default false;

    boolean idempotent() default false;

    int index() default 0;
}
