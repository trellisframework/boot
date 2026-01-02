package net.trellisframework.workflow.temporal.task;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;

import java.util.Arrays;

public interface BaseWorkflowTask {

    default ActivityExecutionContext getContext() {
        return Activity.getExecutionContext();
    }

    default String getTaskToken() {
        return Arrays.toString(getContext().getInfo().getTaskToken());
    }

    default int getAttempt() {
        return getContext().getInfo().getAttempt();
    }

    default void heartbeat(Object details) {
        Activity.getExecutionContext().heartbeat(details);
    }

}
