package net.trellisframework.workflow.temporal.failure;

import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.WorkflowInfo;
import net.trellisframework.core.log.Logger;

import java.util.Optional;

public final class TemporalFailures {

    private TemporalFailures() {
    }

    public static ApplicationFailure activityFailure(Throwable cause, ActivityInfo activity) {
        String failureType = cause.getClass().getName();
        var details = new TemporalFailureDetails(
                TemporalFailureDetails.ACTIVITY_EXECUTION_FAILED,
                failureType,
                activity.getWorkflowId(),
                activity.getRunId(),
                activity.getWorkflowType(),
                activity.getActivityId(),
                activity.getActivityType(),
                activity.getAttempt(),
                failureType,
                activity.getWorkflowId(),
                activity.getRunId(),
                activity.getWorkflowType());
        return ApplicationFailure.newFailureWithCause(details.reason(), details.failureType(), cause, details);
    }

    public static TemporalFailureDetails workflowDetails(Throwable failure, WorkflowInfo workflow, String workflowType) {
        Optional<ApplicationFailure> applicationFailure = findApplicationFailure(failure);
        Optional<ActivityFailure> activityFailure = findActivityFailure(failure);
        Optional<TemporalFailureDetails> activityDetails = applicationFailure.flatMap(TemporalFailures::readActivityDetails);
        String failureType = activityDetails.map(TemporalFailureDetails::failureType)
                .orElseGet(() -> applicationFailure.map(ApplicationFailure::getType).orElseGet(() -> failure.getClass().getName()));
        String reason = activityDetails.map(TemporalFailureDetails::reason)
                .orElse(TemporalFailureDetails.WORKFLOW_EXECUTION_FAILED);
        return new TemporalFailureDetails(
                reason,
                failureType,
                workflow.getWorkflowId(),
                workflow.getRunId(),
                workflowType,
                activityDetails.map(TemporalFailureDetails::activityId).orElseGet(() -> activityFailure.map(ActivityFailure::getActivityId).orElse(null)),
                activityDetails.map(TemporalFailureDetails::activityType).orElseGet(() -> activityFailure.map(ActivityFailure::getActivityType).orElse(null)),
                activityDetails.map(TemporalFailureDetails::attempt).orElseGet(workflow::getAttempt),
                failureType,
                activityDetails.map(TemporalFailureDetails::failureWorkflowId).orElseGet(workflow::getWorkflowId),
                activityDetails.map(TemporalFailureDetails::failureRunId).orElseGet(workflow::getRunId),
                activityDetails.map(TemporalFailureDetails::failureWorkflowType).orElse(workflowType));
    }

    private static Optional<ApplicationFailure> findApplicationFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ApplicationFailure applicationFailure) return Optional.of(applicationFailure);
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static Optional<ActivityFailure> findActivityFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ActivityFailure activityFailure) return Optional.of(activityFailure);
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static Optional<TemporalFailureDetails> readActivityDetails(ApplicationFailure failure) {
        try {
            if (failure.getDetails() != null && failure.getDetails().getSize() > 0)
                return Optional.of(failure.getDetails().get(0, TemporalFailureDetails.class));
            return Optional.empty();
        } catch (RuntimeException decodingFailure) {
            Logger.warn("TemporalFailures",
                    "Could not decode typed activity failure details; using outer failure context (exceptionType=%s)",
                    decodingFailure.getClass().getName());
            return Optional.empty();
        }
    }
}
