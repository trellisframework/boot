package net.trellisframework.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResource {

    String uri();

    String name();

    String description();

    String mimeType() default "text/plain";

    /**
     * Inline value to return directly.
     * Takes priority over {@link #file()} and method invocation.
     */
    String value() default "";

    /**
     * Classpath resource file to serve directly (e.g. "languages.json").
     * When set, the method body is ignored and the file content is returned.
     * JSON files are automatically compacted (parsed and re-serialized).
     */
    String file() default "";
}
