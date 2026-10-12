package net.trellisframework.workflow.temporal.failure;

import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.WorkflowInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemporalFailuresTest {

    private static ActivityInfo activity() {
        ActivityInfo info = mock(ActivityInfo.class);
        when(info.getWorkflowId()).thenReturn("wf-1");
        when(info.getRunId()).thenReturn("run-1");
        when(info.getWorkflowType()).thenReturn("OrderFlow");
        when(info.getActivityId()).thenReturn("act-7");
        when(info.getActivityType()).thenReturn("ChargeCard");
        when(info.getAttempt()).thenReturn(3);
        return info;
    }

    private static WorkflowInfo workflow() {
        WorkflowInfo info = mock(WorkflowInfo.class);
        when(info.getWorkflowId()).thenReturn("parent-1");
        when(info.getRunId()).thenReturn("parent-run");
        when(info.getWorkflowType()).thenReturn("ParentFlow");
        when(info.getAttempt()).thenReturn(1);
        return info;
    }

    @Test
    void activityFailureCarriesStructuredDetailsAndCause() {
        var cause = new IllegalStateException("secret");
        ApplicationFailure failure = TemporalFailures.activityFailure(cause, activity());
        TemporalFailureDetails details = failure.getDetails().get(0, TemporalFailureDetails.class);

        assertEquals(TemporalFailures.ACTIVITY_FAILED, failure.getOriginalMessage());
        assertEquals(IllegalStateException.class.getName(), failure.getType());
        assertSame(cause, failure.getCause());
        assertEquals(new TemporalFailureDetails("wf-1", "run-1", "OrderFlow", "act-7", "ChargeCard", 3, IllegalStateException.class.getName()), details);
    }

    @Test
    void workflowDetailsReuseTheFailingActivityDetails() {
        ApplicationFailure activityFailure = TemporalFailures.activityFailure(new IllegalStateException("boom"), activity());
        TemporalFailureDetails details = TemporalFailures.details(new RuntimeException("wrapped", activityFailure), workflow());

        assertEquals("act-7", details.activityId());
        assertEquals("ChargeCard", details.activityType());
        assertEquals(3, details.attempt());
        assertEquals(IllegalStateException.class.getName(), details.exception());
    }

    @Test
    void workflowDetailsDescribeTheWorkflowWhenNoActivityFailed() {
        TemporalFailureDetails details = TemporalFailures.details(new IllegalArgumentException("bad input"), workflow());

        assertEquals("parent-1", details.workflowId());
        assertEquals("ParentFlow", details.workflowType());
        assertNull(details.activityId());
        assertEquals(1, details.attempt());
        assertEquals(IllegalArgumentException.class.getName(), details.exception());
    }
}
