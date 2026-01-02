package net.trellisframework.workflow.temporal.payload;

import io.temporal.common.Priority;
import lombok.Getter;

/**
 * Options for workflow execution.
 * <p>
 * Priority uses Temporal's standard {@link io.temporal.common.Priority} class:
 * <ul>
 *   <li>1 = Highest priority (tasks run first)</li>
 *   <li>2 = High priority</li>
 *   <li>3 = Normal priority (default)</li>
 *   <li>4 = Low priority</li>
 *   <li>5 = Lowest priority</li>
 * </ul>
 * <p>
 * Examples:
 * <pre>
 * // Simple call (no option) - sync
 * call(MyWorkflow.class, input);
 *
 * // With priority only (1 = highest, 5 = lowest)
 * call(MyWorkflow.class, input, WorkflowOption.of(1));
 *
 * // With concurrency only
 * call(MyWorkflow.class, input, WorkflowOption.of(Concurrency.of("customer-123", 5)));
 *
 * // With priority and concurrency
 * call(MyWorkflow.class, input, WorkflowOption.of(1, Concurrency.of("customer-123", 5)));
 *
 * // With fallback
 * call(MyWorkflow.class, input,
 *     WorkflowOption.of(1, Concurrency.of("customer-123", 5)),
 *     Fallback.of(5, defaultResult)
 * );
 * </pre>
 *
 * @param <O> The return type of the workflow
 */
@Getter
public class WorkflowOption<O> {
    private Priority priority;
    private Concurrency concurrency;

    private WorkflowOption() {
    }

    /**
     * Create option with priority (1-5).
     * <ul>
     *   <li>1 = Highest priority</li>
     *   <li>3 = Normal (default)</li>
     *   <li>5 = Lowest priority</li>
     * </ul>
     *
     * @param priority priority key (1-5, lower = higher priority)
     */
    public static <O> WorkflowOption<O> of(int priority) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.priority = Priority.newBuilder()
                .setPriorityKey(Math.max(1, Math.min(5, priority)))
                .build();
        return option;
    }

    /**
     * Create option with concurrency only.
     */
    public static <O> WorkflowOption<O> of(Concurrency concurrency) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.concurrency = concurrency;
        return option;
    }

    /**
     * Create option with priority and concurrency.
     *
     * @param priority priority key (1-5, lower = higher priority)
     * @param concurrency Concurrency settings
     */
    public static <O> WorkflowOption<O> of(int priority, Concurrency concurrency) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.priority = Priority.newBuilder()
                .setPriorityKey(Math.max(1, Math.min(5, priority)))
                .build();
        option.concurrency = concurrency;
        return option;
    }

    // ============== Getters ==============

    public boolean hasPriority() {
        return priority != null;
    }

    public boolean hasConcurrency() {
        return concurrency != null && concurrency.isValid();
    }

    public String getConcurrencyKey() {
        return concurrency != null ? concurrency.getKey() : null;
    }

    public int getConcurrencyLimit() {
        return concurrency != null ? concurrency.getLimit() : Concurrency.DEFAULT_LIMIT;
    }

}
