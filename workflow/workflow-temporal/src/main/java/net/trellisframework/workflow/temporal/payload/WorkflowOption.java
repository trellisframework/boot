package net.trellisframework.workflow.temporal.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.temporal.common.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowOption {
    private String id;
    private Priority priority;
    private Concurrency concurrency;

    public static WorkflowOption of(String id) {
        WorkflowOption option = new WorkflowOption();
        option.id = id;
        return option;
    }

    public static  WorkflowOption of(int priority) {
        WorkflowOption option = new WorkflowOption();
        option.priority = Priority.newBuilder().setPriorityKey(Math.max(1, Math.min(5, priority))).build();
        return option;
    }

    public static  WorkflowOption of(String id, int priority) {
        WorkflowOption option = new WorkflowOption();
        option.id = id;
        option.priority = Priority.newBuilder().setPriorityKey(Math.max(1, Math.min(5, priority))).build();
        return option;
    }

    public static  WorkflowOption of(Concurrency concurrency) {
        WorkflowOption option = new WorkflowOption();
        option.concurrency = concurrency;
        return option;
    }

    public static  WorkflowOption of(String id, Concurrency concurrency) {
        WorkflowOption option = new WorkflowOption();
        option.id = id;
        option.concurrency = concurrency;
        return option;
    }

    public static  WorkflowOption of(int priority, Concurrency concurrency) {
        WorkflowOption option = new WorkflowOption();
        option.priority = Priority.newBuilder().setPriorityKey(priority).build();
        option.concurrency = concurrency;
        return option;
    }

    public static  WorkflowOption of(String id, int priority, Concurrency concurrency) {
        WorkflowOption option = new WorkflowOption();
        option.id = id;
        option.priority = Priority.newBuilder().setPriorityKey(priority).build();
        option.concurrency = concurrency;
        return option;
    }

    @JsonIgnore
    public boolean hasPriority() {
        return priority != null;
    }

    @JsonIgnore
    public boolean hasConcurrency() {
        return concurrency != null && concurrency.isValid();
    }

    @JsonIgnore
    public String getConcurrencyKey() {
        return concurrency != null ? concurrency.getKey() : null;
    }

    @JsonIgnore
    public int getConcurrencyLimit() {
        return concurrency != null ? concurrency.getLimit() : Concurrency.DEFAULT_LIMIT;
    }

}
