package net.trellisframework.workflow.temporal.failure;

public record TemporalFailureDetails(String workflowId, String runId, String workflowType, String activityId,
                                     String activityType, int attempt, String exception) {
}
