package net.trellisframework.workflow.temporal.provider;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.ChildWorkflowStub;
import io.temporal.workflow.Workflow;
import net.trellisframework.context.process.*;
import net.trellisframework.context.process.Process;
import net.trellisframework.context.provider.ProcessContextProvider;
import net.trellisframework.util.duration.DurationParser;
import net.trellisframework.workflow.temporal.action.*;
import net.trellisframework.workflow.temporal.annotation.Backoff;
import net.trellisframework.workflow.temporal.annotation.Retry;
import net.trellisframework.workflow.temporal.annotation.Task;
import net.trellisframework.workflow.temporal.task.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unchecked")
public interface WorkflowContextProvider extends ProcessContextProvider {

    @Override
    default <TProcess extends Process<O>, O> O call(Class<TProcess> process) {
        if (WorkflowTask.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName());
        }
        if (WorkflowAction.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process);
            return null;
        }
        return ProcessContextProvider.super.call(process);
    }

    @Override
    default <TProcess extends Process1<O, I>, O, I> O call(Class<TProcess> process, I i1) {
        if (WorkflowTask1.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName(), i1);
        }
        if (WorkflowAction1.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process, i1);
            return null;
        }
        return ProcessContextProvider.super.call(process, i1);
    }

    @Override
    default <TProcess extends Process2<O, I1, I2>, O, I1, I2> O call(Class<TProcess> process, I1 i1, I2 i2) {
        if (WorkflowTask2.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName(), i1, i2);
        }
        if (WorkflowAction2.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process, i1, i2);
            return null;
        }
        return ProcessContextProvider.super.call(process, i1, i2);
    }

    @Override
    default <TProcess extends Process3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3) {
        if (WorkflowTask3.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName(), i1, i2, i3);
        }
        if (WorkflowAction3.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process, i1, i2, i3);
            return null;
        }
        return ProcessContextProvider.super.call(process, i1, i2, i3);
    }

    @Override
    default <TProcess extends Process4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4) {
        if (WorkflowTask4.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName(), i1, i2, i3, i4);
        }
        if (WorkflowAction4.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process, i1, i2, i3, i4);
            return null;
        }
        return ProcessContextProvider.super.call(process, i1, i2, i3, i4);
    }

    @Override
    default <TProcess extends Process5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        if (WorkflowTask5.class.isAssignableFrom(process)) {
            return executeTask(process, process.getName(), i1, i2, i3, i4, i5);
        }
        if (WorkflowAction5.class.isAssignableFrom(process) && isAsync(process)) {
            startChildWorkflow(process, i1, i2, i3, i4, i5);
            return null;
        }
        return ProcessContextProvider.super.call(process, i1, i2, i3, i4, i5);
    }

    default <TAction extends WorkflowAction<?>> void callAsync(Class<TAction> action) {
        startChildWorkflow(action);
    }

    default <TAction extends WorkflowAction1<?, I>, I> void callAsync(Class<TAction> action, I i1) {
        startChildWorkflow(action, i1);
    }

    default <TAction extends WorkflowAction2<?, I1, I2>, I1, I2> void callAsync(Class<TAction> action, I1 i1, I2 i2) {
        startChildWorkflow(action, i1, i2);
    }

    default <TAction extends WorkflowAction3<?, I1, I2, I3>, I1, I2, I3> void callAsync(Class<TAction> action, I1 i1, I2 i2, I3 i3) {
        startChildWorkflow(action, i1, i2, i3);
    }

    default <TAction extends WorkflowAction4<?, I1, I2, I3, I4>, I1, I2, I3, I4> void callAsync(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4) {
        startChildWorkflow(action, i1, i2, i3, i4);
    }

    default <TAction extends WorkflowAction5<?, I1, I2, I3, I4, I5>, I1, I2, I3, I4, I5> void callAsync(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        startChildWorkflow(action, i1, i2, i3, i4, i5);
    }

    private boolean isAsync(Class<?> clazz) {
        return clazz.isAnnotationPresent(net.trellisframework.workflow.temporal.annotation.Async.class);
    }

    private <O> O executeTask(Class<?> taskClass, Object... args) {
        var stub = Workflow.newUntypedActivityStub(buildActivityOptions(taskClass));
        return (O) stub.execute(taskClass.getSimpleName(), Object.class, args);
    }

    private void startChildWorkflow(Class<?> workflowClass, Object... args) {
        var annotation = workflowClass.getAnnotation(net.trellisframework.workflow.temporal.annotation.Workflow.class);
        String defaultTimout = net.trellisframework.workflow.temporal.annotation.Workflow.DEFAULT_TIMEOUT;
        String defaultTaskQueue = Workflow.getInfo().getTaskQueue();
        String taskQueue = annotation != null && !annotation.taskQueue().isBlank() ? annotation.taskQueue() : defaultTaskQueue;
        Duration timeout = Optional.ofNullable(annotation).map(x -> DurationParser.parse(x.timeout())).orElse(DurationParser.parse(defaultTimout));
        String workflowId = workflowClass.getSimpleName() + "-" + UUID.randomUUID();
        ChildWorkflowStub stub = Workflow.newUntypedChildWorkflowStub(
                "DynamicWorkflowAction",
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(taskQueue)
                        .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                        .setWorkflowExecutionTimeout(timeout)
                        .build()
        );
        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = workflowClass.getName();
        System.arraycopy(args, 0, allArgs, 1, args.length);
        Async.function(() -> stub.execute(Object.class, allArgs));
        stub.getExecution().get();
    }

    private ActivityOptions buildActivityOptions(Class<?> taskClass) {
        Task task = taskClass.getAnnotation(Task.class);
        Duration timeout = task != null ? DurationParser.parse(task.timeout()) : Duration.ofMinutes(5);
        ActivityOptions.Builder builder = ActivityOptions.newBuilder().setStartToCloseTimeout(timeout);
        if (task != null) {
            Optional.ofNullable(task.heartbeat()).ifPresent(x -> builder.setHeartbeatTimeout(DurationParser.parse(x)));
            Retry retry = task.retry();
            Backoff backoff = retry.backoff();
            RetryOptions.Builder retryBuilder = RetryOptions.newBuilder()
                    .setMaximumAttempts(retry.maxAttempts())
                    .setInitialInterval(Duration.ofMillis(backoff.delay()))
                    .setMaximumInterval(Duration.ofMillis(backoff.maxDelay()))
                    .setBackoffCoefficient(backoff.multiplier());

            if (retry.exclude().length > 0) {
                String[] exceptionNames = Arrays.stream(retry.exclude()).map(Class::getName).toArray(String[]::new);
                retryBuilder.setDoNotRetry(exceptionNames);
            }
            builder.setRetryOptions(retryBuilder.build());
        } else {
            builder.setHeartbeatTimeout(Duration.ofSeconds(10));
            builder.setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build());
        }
        return builder.build();
    }

}
