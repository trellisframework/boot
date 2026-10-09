package net.trellisframework.workflow.temporal.failure;

import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.WorkflowInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemporalFailuresTest {

    @Test
    void activityFailureUsesTypedSafeDetailsAndOmitsRawExceptionMessage() {
        ActivityInfo activity = mock(ActivityInfo.class);
        when(activity.getWorkflowId()).thenReturn("child-workflow-123");
        when(activity.getRunId()).thenReturn("child-run-456");
        when(activity.getWorkflowType()).thenReturn("child-workflow");
        when(activity.getActivityId()).thenReturn("activity-789");
        when(activity.getActivityType()).thenReturn("settle-prospect");
        when(activity.getAttempt()).thenReturn(2);
        var exception = new IllegalStateException("secret-token-do-not-copy");

        ApplicationFailure failure = TemporalFailures.activityFailure(exception, activity);
        TemporalFailureDetails details = failure.getDetails().get(0, TemporalFailureDetails.class);

        assertEquals(IllegalStateException.class.getName(), failure.getType());
        assertEquals(TemporalFailureDetails.ACTIVITY_EXECUTION_FAILED, failure.getOriginalMessage());
        assertEquals("child-workflow-123", details.workflowId());
        assertEquals("child-run-456", details.runId());
        assertEquals("child-workflow-123", details.failureWorkflowId());
        assertEquals("activity-789", details.activityId());
        assertEquals("settle-prospect", details.activityType());
        assertEquals(2, details.attempt());
        assertFalse(failure.getOriginalMessage().contains("secret-token"));
        assertSame(exception, failure.getCause());
    }

    @Test
    void workflowDetailsDoNotExposeUnhandledExceptionMessage() {
        WorkflowInfo workflow = mock(WorkflowInfo.class);
        when(workflow.getWorkflowId()).thenReturn("parent-workflow-123");
        when(workflow.getRunId()).thenReturn("parent-run-456");
        when(workflow.getWorkflowType()).thenReturn("parent-workflow");
        when(workflow.getAttempt()).thenReturn(1);

        var details = TemporalFailures.workflowDetails(
                new IllegalArgumentException("private input value"), workflow, "prospect-workflow");

        assertEquals(TemporalFailureDetails.WORKFLOW_EXECUTION_FAILED, details.reason());
        assertEquals(IllegalArgumentException.class.getName(), details.failureType());
        assertEquals("parent-workflow-123", details.workflowId());
        assertEquals("parent-run-456", details.runId());
        assertEquals(1, details.attempt());
        assertFalse(details.reason().contains("private input value"));
    }

    @Test
    void workflowUiDetailsPreserveSafeActivityFailureContext() {
        ActivityInfo activity = mock(ActivityInfo.class);
        when(activity.getWorkflowId()).thenReturn("child-workflow-123");
        when(activity.getRunId()).thenReturn("child-run-456");
        when(activity.getWorkflowType()).thenReturn("child-workflow");
        when(activity.getActivityId()).thenReturn("activity-789");
        when(activity.getActivityType()).thenReturn("settle-prospect");
        when(activity.getAttempt()).thenReturn(2);
        WorkflowInfo workflow = mock(WorkflowInfo.class);
        when(workflow.getWorkflowId()).thenReturn("parent-workflow-123");
        when(workflow.getRunId()).thenReturn("parent-run-456");

        ApplicationFailure activityFailure = TemporalFailures.activityFailure(
                new IllegalStateException("sensitive payload"), activity);
        var uiDetails = TemporalFailures.workflowDetails(activityFailure, workflow, "prospect-workflow");

        assertEquals(TemporalFailureDetails.ACTIVITY_EXECUTION_FAILED, uiDetails.reason());
        assertEquals(IllegalStateException.class.getName(), uiDetails.failureType());
        assertEquals("parent-workflow-123", uiDetails.workflowId());
        assertEquals("parent-run-456", uiDetails.runId());
        assertEquals("child-workflow-123", uiDetails.failureWorkflowId());
        assertEquals("child-run-456", uiDetails.failureRunId());
        assertEquals("child-workflow", uiDetails.failureWorkflowType());
        assertEquals("activity-789", uiDetails.activityId());
        assertEquals("settle-prospect", uiDetails.activityType());
        assertEquals(2, uiDetails.attempt());
    }
}
