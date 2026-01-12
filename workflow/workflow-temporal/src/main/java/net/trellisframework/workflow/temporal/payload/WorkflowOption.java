package net.trellisframework.workflow.temporal.payload;

import io.temporal.common.Priority;
import lombok.Getter;


@Getter
public class WorkflowOption<O> {
    private Priority priority;
    private Concurrency concurrency;

    public static <O> WorkflowOption<O> of(int priority) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.priority = Priority.newBuilder().setPriorityKey(Math.max(1, Math.min(5, priority))).build();
        return option;
    }

    public static <O> WorkflowOption<O> of(Concurrency concurrency) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.concurrency = concurrency;
        return option;
    }

    public static <O> WorkflowOption<O> of(int priority, Concurrency concurrency) {
        WorkflowOption<O> option = new WorkflowOption<>();
        option.priority = Priority.newBuilder().setPriorityKey(Math.max(1, Math.min(5, priority))).build();
        option.concurrency = concurrency;
        return option;
    }

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
