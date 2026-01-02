package net.trellisframework.workflow.temporal.payload;

import lombok.Getter;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Fallback configuration for workflow execution.
 * <p>
 * Used when:
 * - Timeout is reached
 * - Concurrency limit is exceeded
 * <p>
 * Examples:
 * <pre>
 * // Async (fire and forget) - timeout=0 means return immediately
 * call(MyWorkflow.class, input, Fallback.of(0, defaultResult));
 *
 * // With timeout and fallback value
 * call(MyWorkflow.class, input, Fallback.of(5, defaultResult));
 *
 * // With timeout and fallback supplier
 * call(MyWorkflow.class, input, Fallback.of(Duration.ofSeconds(5), () -> computeDefault()));
 *
 * // Combined with option
 * call(MyWorkflow.class, input,
 *     WorkflowOption.of(10, "key", 5),
 *     Fallback.of(5, defaultResult)
 * );
 * </pre>
 *
 * @param <O> The return type of the workflow
 */
@Getter
public class Fallback<O> {
    private Duration timeout;
    private Supplier<O> value;

    private Fallback() {
    }

    /**
     * Create fallback with timeout in seconds and value.
     * timeout=0 means async (fire and forget) - returns value immediately
     */
    public static <O> Fallback<O> of(long timeoutSeconds, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = Duration.ofSeconds(timeoutSeconds);
        fallback.value = () -> value;
        return fallback;
    }

    /**
     * Create fallback with timeout in seconds (nullable) and value.
     * timeout=0 means async (fire and forget) - returns value immediately
     * timeout=null means sync (wait forever)
     */
    public static <O> Fallback<O> of(Integer timeoutSeconds, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
        fallback.value = () -> value;
        return fallback;
    }

    /**
     * Create fallback with timeout in seconds and value supplier.
     * timeout=0 means async (fire and forget) - returns value immediately
     */
    public static <O> Fallback<O> of(long timeoutSeconds, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = Duration.ofSeconds(timeoutSeconds);
        fallback.value = value;
        return fallback;
    }

    /**
     * Create fallback with timeout in seconds (nullable) and value supplier.
     * timeout=0 means async (fire and forget) - returns value immediately
     * timeout=null means sync (wait forever)
     */
    public static <O> Fallback<O> of(Integer timeoutSeconds, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
        fallback.value = value;
        return fallback;
    }

    /**
     * Create fallback with Duration and value.
     * Duration.ZERO means async (fire and forget) - returns value immediately
     */
    public static <O> Fallback<O> of(Duration timeout, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeout;
        fallback.value = () -> value;
        return fallback;
    }

    /**
     * Create fallback with Duration and value supplier.
     * Duration.ZERO means async (fire and forget) - returns value immediately
     */
    public static <O> Fallback<O> of(Duration timeout, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeout;
        fallback.value = value;
        return fallback;
    }

    /**
     * Check if timeout is set and greater than zero.
     * Note: timeout=0 is valid (means async), but this returns false for it.
     */
    public boolean hasTimeout() {
        return timeout != null && !timeout.isZero();
    }

    /**
     * Check if timeout is set (including zero).
     * timeout=0 means async (fire and forget)
     */
    public boolean hasTimeoutOrAsync() {
        return timeout != null;
    }

    /**
     * Check if this is an async (fire and forget) fallback.
     * timeout=0 means async.
     */
    public boolean isAsync() {
        return timeout != null && timeout.isZero();
    }

    public boolean hasValue() {
        return value != null;
    }
}
