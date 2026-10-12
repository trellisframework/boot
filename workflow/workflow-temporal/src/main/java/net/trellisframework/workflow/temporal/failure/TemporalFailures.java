package net.trellisframework.workflow.temporal.failure;

import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.WorkflowInfo;
import org.slf4j.MDC;

import java.util.Optional;

public final class TemporalFailures {

    public static final String ACTIVITY_FAILED = "Activity execution failed";

    private TemporalFailures() {
    }

    public static ApplicationFailure activityFailure(Throwable cause, ActivityInfo info) {
        var details = new TemporalFailureDetails(info.getWorkflowId(), info.getRunId(), info.getWorkflowType(),
                info.getActivityId(), info.getActivityType(), info.getAttempt(), cause.getClass().getName());
        return ApplicationFailure.newFailureWithCause(ACTIVITY_FAILED, details.exception(), cause, details);
    }

    public static TemporalFailureDetails details(Throwable failure, WorkflowInfo info) {
        for (Throwable cause = failure; cause != null && cause.getCause() != cause; cause = cause.getCause())
            if (cause instanceof ApplicationFailure application && application.getDetails().getSize() > 0)
                return application.getDetails().get(0, TemporalFailureDetails.class);
        String workflowType = Optional.ofNullable(MDC.get("WorkflowType")).orElseGet(info::getWorkflowType);
        return new TemporalFailureDetails(info.getWorkflowId(), info.getRunId(), workflowType, null, null, info.getAttempt(), failure.getClass().getName());
    }
}
