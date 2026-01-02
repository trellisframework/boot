package net.trellisframework.workflow.temporal.action;

import io.temporal.workflow.Workflow;
import net.trellisframework.workflow.temporal.provider.WorkflowContextProvider;

import java.time.Duration;

public interface BaseWorkflowAction extends WorkflowContextProvider {

    default void sleep(Duration duration) {
        Workflow.sleep(duration);
    }

    default void sleep(long millis) {
        Workflow.sleep(Duration.ofMillis(millis));
    }

    default void sleepMinutes(long minutes) {
        Workflow.sleep(Duration.ofMinutes(minutes));
    }

    default String getWorkflowId() {
        return Workflow.getInfo().getWorkflowId();
    }

    default int getAttempt() {
        return Workflow.getInfo().getAttempt();
    }

    default int version(String changeId, int maxVersion) {
        return Workflow.getVersion(changeId, Workflow.DEFAULT_VERSION, maxVersion);
    }

    default boolean isVersion(String changeId, int minVersion) {
        return version(changeId, minVersion) >= minVersion;
    }

}
