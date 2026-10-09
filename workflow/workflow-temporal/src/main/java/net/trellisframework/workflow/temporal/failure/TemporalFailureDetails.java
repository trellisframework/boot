package net.trellisframework.workflow.temporal.failure;

public record TemporalFailureDetails(
        String reason,
        String failureType,
        String workflowId,
        String runId,
        String workflowType,
        String activityId,
        String activityType,
        int attempt,
        String exceptionType,
        String failureWorkflowId,
        String failureRunId,
        String failureWorkflowType) {

    public static final String ACTIVITY_EXECUTION_FAILED = "Activity execution failed";
    public static final String WORKFLOW_EXECUTION_FAILED = "Workflow execution failed";
}
