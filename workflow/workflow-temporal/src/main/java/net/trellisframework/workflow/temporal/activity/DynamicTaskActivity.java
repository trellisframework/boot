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
import net.trellisframework.workflow.temporal.action.*;
import net.trellisframework.workflow.temporal.annotation.Retry;
import net.trellisframework.workflow.temporal.task.*;
import net.trellisframework.workflow.temporal.util.TypeResolver;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DynamicTaskActivity implements DynamicActivity {

    @Override
    public Object execute(EncodedValues args) {
        String className = args.get(0, String.class);
        ScheduledExecutorService heartbeatScheduler = null;
        try {
            Class<?> clazz = Class.forName(className);
            Object task = ApplicationContextProvider.context.getBean(clazz);
            Class<?> baseInterface = BaseWorkflowRepositoryTask.class.isAssignableFrom(clazz)
                    ? BaseWorkflowRepositoryTask.class
                    : BaseWorkflowAction.class.isAssignableFrom(clazz)
                    ? BaseWorkflowAction.class
                    : BaseWorkflowTask.class;
            Type[] paramTypes = TypeResolver.getGenericParameterTypes(clazz, baseInterface);
            heartbeatScheduler = startHeartbeat(clazz);
            try {
                return executeTask(task, args, paramTypes);
            } catch (Exception e) {
                throw toApplicationFailure(e, clazz);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Task class not found: " + className, e);
        } finally {
            stopHeartbeat(heartbeatScheduler);
        }
    }

    private Object executeTask(Object task, EncodedValues args, Type[] paramTypes) {
        return switch (task) {
            case WorkflowTask<?> t -> t.execute();
            case WorkflowTask1 t -> t.execute(arg(args, 1, paramTypes, 0));
            case WorkflowTask2 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1));
            case WorkflowTask3 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2));
            case WorkflowTask4 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3));
            case WorkflowTask5 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3), arg(args, 5, paramTypes, 4));
            case WorkflowRepositoryTask t -> t.execute();
            case WorkflowRepositoryTask1 t -> t.execute(arg(args, 1, paramTypes, 0));
            case WorkflowRepositoryTask2 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1));
            case WorkflowRepositoryTask3 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2));
            case WorkflowRepositoryTask4 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3));
            case WorkflowRepositoryTask5 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3), arg(args, 5, paramTypes, 4));
            case WorkflowAction<?> t -> t.execute();
            case WorkflowAction1 t -> t.execute(arg(args, 1, paramTypes, 0));
            case WorkflowAction2 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1));
            case WorkflowAction3 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2));
            case WorkflowAction4 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3));
            case WorkflowAction5 t -> t.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3), arg(args, 5, paramTypes, 4));
            default -> throw new IllegalArgumentException("Unknown task type: " + task.getClass().getName());
        };
    }

    private Object arg(EncodedValues args, int argIndex, Type[] paramTypes, int typeIndex) {
        return TypeResolver.convert(args.get(argIndex, Object.class), paramTypes[typeIndex]);
    }

    private ScheduledExecutorService startHeartbeat(Class<?> clazz) {
        var annotation = clazz.getAnnotation(net.trellisframework.workflow.temporal.annotation.Activity.class);
        Duration interval = annotation != null ? DurationParser.parse(annotation.heartbeat()) : Duration.ofSeconds(10);
        ActivityExecutionContext ctx = Activity.getExecutionContext();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-" + clazz.getSimpleName() + "-" + Thread.currentThread().getId());
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
        var annotation = taskClass.getAnnotation(net.trellisframework.workflow.temporal.annotation.Activity.class);
        Retry retry = annotation != null ? annotation.retry() : null;
        String message = e instanceof HttpException http ? JsonUtil.toString(http.getErrorMessage()) : e.getMessage();
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        Logger.error(taskClass.getSimpleName(), "Task failed (attempt %d): %s", attempt, message);
        boolean nonRetryable = Optional.ofNullable(retry).map(x -> x.include().length).orElse(0) > 0 && Arrays.stream(retry.include()).noneMatch(type -> type.isInstance(e));
        ApplicationFailure failure = ApplicationFailure.newFailureWithCause(message, e.getClass().getName(), null);
        failure.setNonRetryable(nonRetryable);
        failure.setStackTrace(new StackTraceElement[0]);
        return failure;
    }
}
