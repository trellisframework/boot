package net.trellisframework.workflow.temporal.logging;

import io.temporal.activity.ActivityInfo;
import io.temporal.activity.Activity;
import io.temporal.workflow.WorkflowInfo;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import net.trellisframework.core.log.LogContext;
import net.trellisframework.core.log.LogContextProvider;
import net.trellisframework.workflow.temporal.failure.TemporalFailureDetails;

import java.util.Optional;

public final class TemporalLogContextProvider implements LogContextProvider {

    @Override
    public Optional<LogContext> resolve() {
        if (WorkflowUnsafe.isWorkflowThread()) {
            return getWorkflowContext()
                    .map(context -> new LogContext(context, WorkflowUnsafe.isReplaying()));
        }
        Optional<String> activityContext = getActivityContext();
        return activityContext.map(context -> new LogContext(context, false));
    }

    public static String getContext(TemporalFailureDetails details) {
        WorkflowInfo info = Workflow.getInfo();
        return "Workflow execution failed (workflowId=%s runId=%s workflowType=%s failureWorkflowId=%s failureRunId=%s failureWorkflowType=%s failureType=%s activityType=%s activityId=%s attempt=%d)"
                .formatted(info.getWorkflowId(), info.getRunId(), info.getWorkflowType(),
                        details.failureWorkflowId(), details.failureRunId(), details.failureWorkflowType(),
                        details.failureType(), details.activityType(), details.activityId(), details.attempt());
    }

    private static Optional<String> getActivityContext() {
        try {
            ActivityInfo info = Activity.getExecutionContext().getInfo();
            return Optional.of("workflowId=%s runId=%s workflowType=%s activityId=%s activityType=%s attempt=%s"
                    .formatted(info.getWorkflowId(), info.getRunId(), info.getWorkflowType(),
                            info.getActivityId(), info.getActivityType(), info.getAttempt()));
        } catch (IllegalStateException notInActivity) {
            return Optional.empty();
        }
    }

    private static Optional<String> getWorkflowContext() {
        try {
            WorkflowInfo info = Workflow.getInfo();
            return Optional.of("workflowId=%s runId=%s workflowType=%s attempt=%s"
                    .formatted(info.getWorkflowId(), info.getRunId(), info.getWorkflowType(), info.getAttempt()));
        } catch (IllegalStateException notInWorkflow) {
            return Optional.empty();
        }
    }
}
