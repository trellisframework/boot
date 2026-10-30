package net.trellisframework.workflow.temporal.failure;

import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.TemporalFailure;
import io.temporal.workflow.WorkflowInfo;
import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.util.json.JsonUtil;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

public final class TemporalFailures {

    public static final String ACTIVITY_FAILED = "Activity execution failed";

    private TemporalFailures() {
    }

    public static TemporalFailure asTemporalFailure(Exception exception) {
        if (exception instanceof TemporalFailure temporalFailure)
            return temporalFailure;
        return ApplicationFailure.newFailureWithCause(
                exception.getMessage(), exception.getClass().getName(), exception);
    }

    /*
        This message handler must be handled of the ControllerAdvice level.
        This is here to keep the backward compatibility with the current version of jumbo.
        On the nect iteration this will be removed.
     */
    public static String message(ApplicationFailure failure) {
        var httpError = httpErrorMessage(failure).map(HttpErrorMessage::getError).orElse(null);
        if (httpError != null && !httpError.isBlank())
            return httpError;
        var message = failure.getOriginalMessage();
        if (message == null || !message.startsWith("{"))
            return message;
        return Optional.ofNullable(JsonUtil.toObject(message, Map.class))
                .map(json -> json.get("error"))
                .map(String::valueOf)
                .orElse(message);
    }


    public static Optional<HttpErrorMessage> httpErrorMessage(ApplicationFailure failure) {
        if (failure.getDetails().getSize() < 2)
            return Optional.empty();
        try {
            return Optional.ofNullable(failure.getDetails().get(1, HttpErrorMessage.class));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static String describeFailure(Throwable error, String temporalFailureMessage) {
        Throwable cause = error;
        while (cause != null && !(cause instanceof ApplicationFailure) && cause.getCause() != cause)
            cause = cause.getCause();
        if (cause instanceof ApplicationFailure failure)
            return message(failure);
        return error instanceof TemporalFailure ? temporalFailureMessage : error.getMessage();
    }

    public static ApplicationFailure activityFailure(Throwable cause, ActivityInfo info) {
        var details = new TemporalFailureDetails(info.getWorkflowId(), info.getRunId(), info.getWorkflowType(),
                info.getActivityId(), info.getActivityType(), info.getAttempt(), cause.getClass().getName());
        if (cause instanceof HttpException http && http.getErrorMessage() != null)
            return ApplicationFailure.newFailureWithCause(ACTIVITY_FAILED, details.exception(), cause, details, http.getErrorMessage());
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
