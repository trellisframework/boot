package net.trellisframework.workflow.temporal.payload;

import lombok.Getter;

/**
 * Concurrency configuration for workflow execution.
 * <p>
 * Examples:
 * <pre>
 * // With key and limit
 * Concurrency.of("customer-123", 5)
 *
 * // With key and default limit (10)
 * Concurrency.of("customer-123")
 * </pre>
 */
@Getter
public class Concurrency {
    public static final int DEFAULT_LIMIT = 10;
    private String key;
    private int limit;

    private Concurrency() {
    }

    /**
     * Create concurrency with key and limit.
     */
    public static Concurrency of(String key, int limit) {
        Concurrency concurrency = new Concurrency();
        concurrency.key = key;
        concurrency.limit = limit;
        return concurrency;
    }

    /**
     * Create concurrency with key and limit (nullable).
     */
    public static Concurrency of(String key, Integer limit) {
        Concurrency concurrency = new Concurrency();
        concurrency.key = key;
        concurrency.limit = limit != null ? limit : DEFAULT_LIMIT;
        return concurrency;
    }

    /**
     * Create concurrency with key and default limit (10).
     */
    public static Concurrency of(String key) {
        return of(key, DEFAULT_LIMIT);
    }

    public boolean isValid() {
        return key != null && !key.isEmpty();
    }
}

