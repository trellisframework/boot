package net.trellisframework.workflow.temporal.payload;

import lombok.Getter;

import java.time.Duration;
import java.util.function.Supplier;

@Getter
public class Fallback<O> {
    private Duration timeout;
    private Supplier<O> value;

    public static <O> Fallback<O> of(long timeoutSeconds, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = Duration.ofSeconds(timeoutSeconds);
        fallback.value = () -> value;
        return fallback;
    }

    public static <O> Fallback<O> of(Integer timeoutSeconds, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
        fallback.value = () -> value;
        return fallback;
    }

    public static <O> Fallback<O> of(long timeoutSeconds, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = Duration.ofSeconds(timeoutSeconds);
        fallback.value = value;
        return fallback;
    }

    public static <O> Fallback<O> of(Integer timeoutSeconds, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null;
        fallback.value = value;
        return fallback;
    }

    public static <O> Fallback<O> of(Duration timeout, O value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeout;
        fallback.value = () -> value;
        return fallback;
    }

    public static <O> Fallback<O> of(Duration timeout, Supplier<O> value) {
        Fallback<O> fallback = new Fallback<>();
        fallback.timeout = timeout;
        fallback.value = value;
        return fallback;
    }

    public boolean hasTimeout() {
        return timeout != null && !timeout.isZero();
    }

    public boolean hasTimeoutOrAsync() {
        return timeout != null;
    }

    public boolean isAsync() {
        return timeout != null && timeout.isZero();
    }

    public boolean hasValue() {
        return value != null;
    }
}
