package net.trellisframework.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as an MCP resource endpoint (similar to Spring Data Repository).
 * Methods annotated with {@link McpResource} are automatically registered.
 * Each method must have either {@link McpResource#file()} or {@link McpResource#value()} set.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResourceEndpoint {
}
