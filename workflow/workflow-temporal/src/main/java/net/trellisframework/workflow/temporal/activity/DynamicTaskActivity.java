package net.trellisframework.workflow.temporal.activity;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;
import io.temporal.failure.ApplicationFailure;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.log.Logger;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.util.duration.DurationParser;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.workflow.temporal.annotation.Retry;
import net.trellisframework.workflow.temporal.annotation.Task;
import net.trellisframework.workflow.temporal.task.*;
import net.trellisframework.workflow.temporal.util.TypeResolver;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DynamicTaskActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        String taskClassName = args.get(0, String.class);
        ScheduledExecutorService heartbeatScheduler = null;

        try {
            Class<?> taskClass = Class.forName(taskClassName);
            Object task = ApplicationContextProvider.context.getBean(taskClass);
            Class<?>[] paramTypes = TypeResolver.getParameterTypes(taskClass, BaseWorkflowTask.class);

            heartbeatScheduler = startHeartbeat(taskClass);
            try {
                return executeTask(task, args, paramTypes);
            } catch (Exception e) {
                throw toApplicationFailure(e, taskClass);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Task class not found: " + taskClassName, e);
        } finally {
            stopHeartbeat(heartbeatScheduler);
        }
    }

    private Object executeTask(Object task, EncodedValues args, Class<?>[] paramTypes) {
        return switch (task) {
            case WorkflowTask<?> t -> t.execute();
            case WorkflowTask1 t -> t.execute(arg(args, 1, paramTypes, 0));
            case WorkflowTask2 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1));
            case WorkflowTask3 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2));
            case WorkflowTask4 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3));
            case WorkflowTask5 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3), arg(args, 5, paramTypes, 4));
            default -> throw new IllegalArgumentException("Unknown task type: " + task.getClass().getName());
        };
    }

    private Object arg(EncodedValues args, int argIndex, Class<?>[] paramTypes, int typeIndex) {
        return TypeResolver.convert(args.get(argIndex, Object.class), paramTypes[typeIndex]);
    }

    private ScheduledExecutorService startHeartbeat(Class<?> taskClass) {
        Task annotation = taskClass.getAnnotation(Task.class);
        Duration interval = annotation != null ? DurationParser.parse(annotation.heartbeat()) : Duration.ofSeconds(10);
        ActivityExecutionContext ctx = Activity.getExecutionContext();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-" + taskClass.getSimpleName() + "-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ctx.heartbeat(null);
            } catch (Exception ignored) {
            }
        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);

        return scheduler;
    }

    private void stopHeartbeat(ScheduledExecutorService scheduler) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private RuntimeException toApplicationFailure(Exception e, Class<?> taskClass) {
        Task annotation = taskClass.getAnnotation(Task.class);
        Retry retry = annotation != null ? annotation.retry() : null;
        String message = e instanceof HttpException http ? JsonUtil.toString(http.getErrorMessage()) : e.getMessage();
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        Logger.error(taskClass.getSimpleName(), "Task failed (attempt %d): %s", attempt, message);

        boolean nonRetryable = retry != null && retry.include().length > 0
                && Arrays.stream(retry.include()).noneMatch(type -> type.isInstance(e));

        ApplicationFailure failure = ApplicationFailure.newFailureWithCause(message, e.getClass().getName(), null);
        failure.setNonRetryable(nonRetryable);
        failure.setStackTrace(new StackTraceElement[0]);
        return failure;
    }
}
