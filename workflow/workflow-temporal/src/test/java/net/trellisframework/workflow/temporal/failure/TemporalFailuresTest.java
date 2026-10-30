package net.trellisframework.workflow.temporal.failure;

import io.temporal.api.enums.v1.TimeoutType;
import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.TimeoutFailure;
import io.temporal.failure.TemporalFailure;
import io.temporal.workflow.WorkflowInfo;
import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemporalFailuresTest {

    @Test
    void messageExtractsReadableErrorFromJsonApplicationFailure() {
        var failure = ApplicationFailure.newFailure(
                "{\"error\":\"service unavailable\",\"httpStatus\":\"503 SERVICE_UNAVAILABLE\"}",
                "ServiceUnavailableException");

        assertEquals("service unavailable", TemporalFailures.message(failure));
    }

    @Test
    void messageKeepsPlainAndUnrecognizedApplicationFailureMessages() {
        var plainFailure = ApplicationFailure.newFailure("timed out", "TimeoutException");
        var jsonWithoutError = ApplicationFailure.newFailure("{\"status\":503}", "ServiceFailure");

        assertEquals("timed out", TemporalFailures.message(plainFailure));
        assertEquals("{\"status\":503}", TemporalFailures.message(jsonWithoutError));
    }

    @Test
    void describeFailureFindsNestedApplicationFailure() {
        var applicationFailure = ApplicationFailure.newFailure(
                "{\"error\":\"service unavailable\"}", "ServiceUnavailableException");

        assertEquals("service unavailable", TemporalFailures.describeFailure(
                new RuntimeException("activity failed", applicationFailure), "prospect timed out"));
    }

    @Test
    void describeFailureUsesProvidedTimeoutTextForTemporalFailure() {
        var failure = new TimeoutFailure("timed out", null, TimeoutType.TIMEOUT_TYPE_START_TO_CLOSE);

        assertEquals("prospect timed out", TemporalFailures.describeFailure(failure, "prospect timed out"));
    }

    @Test
    void describeFailureUsesMessageForRegularException() {
        assertEquals("invalid request", TemporalFailures.describeFailure(
                new IllegalArgumentException("invalid request"), "prospect timed out"));
    }

    @Test
    void asTemporalFailurePreservesExistingTemporalFailure() {
        TemporalFailure existing = ApplicationFailure.newFailure("already temporal", "ExistingFailure");

        assertSame(existing, TemporalFailures.asTemporalFailure((Exception) existing));
    }

    @Test
    void asTemporalFailureWrapsRegularExceptionWithMessageTypeAndCause() {
        var cause = new IllegalArgumentException("invalid request");

        var failure = assertInstanceOf(ApplicationFailure.class, TemporalFailures.asTemporalFailure(cause));

        assertEquals("invalid request", failure.getOriginalMessage());
        assertEquals(IllegalArgumentException.class.getName(), failure.getType());
        assertSame(cause, failure.getCause());
    }

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
    void activityFailureKeepsGenericTemporalMessageAndCarriesReadableHttpErrorSeparately() {
        var error = new HttpErrorMessage();
        error.setHttpStatus(HttpStatus.PAYMENT_REQUIRED);
        error.setStatus(402);
        error.setError("Sorry, you don’t have enough credit");
        HttpException cause = new HttpException(error);

        ApplicationFailure failure = TemporalFailures.activityFailure(cause, activity());

        assertEquals(TemporalFailures.ACTIVITY_FAILED, failure.getOriginalMessage());
        assertEquals("Sorry, you don’t have enough credit", TemporalFailures.message(failure));
        assertEquals("Sorry, you don’t have enough credit", TemporalFailures.describeFailure(
                new RuntimeException("workflow failed", failure), "prospect timed out"));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, TemporalFailures.httpErrorMessage(failure).orElseThrow().getHttpStatus());
        assertEquals(402, TemporalFailures.httpErrorMessage(failure).orElseThrow().getStatus());
        assertSame(cause, failure.getCause());
        assertEquals(TemporalFailureDetails.class,
                failure.getDetails().get(0, TemporalFailureDetails.class).getClass());
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
