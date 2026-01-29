package net.trellisframework.workflow.temporal.provider;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.*;
import net.trellisframework.context.process.*;
import net.trellisframework.context.process.Process;
import net.trellisframework.context.provider.ProcessContextProvider;
import net.trellisframework.util.duration.DurationParser;
import net.trellisframework.util.string.StringUtil;
import net.trellisframework.workflow.temporal.action.*;
import net.trellisframework.workflow.temporal.annotation.Activity;
import net.trellisframework.workflow.temporal.payload.ClosePolicy;
import net.trellisframework.workflow.temporal.payload.WorkflowOption;
import net.trellisframework.workflow.temporal.util.TypeResolver;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public interface WorkflowContextProvider extends ProcessContextProvider {

    @Override
    default <TProcess extends Process<O>, O> O call(Class<TProcess> process) {
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process);
        if (WorkflowAction.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process);
        return ProcessContextProvider.super.call(process);
    }

    @Override
    default <TProcess extends Process1<O, I>, O, I> O call(Class<TProcess> process, I i1) {
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process, i1);
        if (WorkflowAction1.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process, i1);
        return ProcessContextProvider.super.call(process, i1);
    }

    @Override
    default <TProcess extends Process2<O, I1, I2>, O, I1, I2> O call(Class<TProcess> process, I1 i1, I2 i2) {
        if (i2 instanceof WorkflowOption option && WorkflowAction1.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflowWithOption(process, option, i1);
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process, i1, i2);
        if (WorkflowAction2.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process, i1, i2);
        return ProcessContextProvider.super.call(process, i1, i2);
    }

    @Override
    default <TProcess extends Process3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3) {
        if (i3 instanceof WorkflowOption option && WorkflowAction2.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflowWithOption(process, option, i1, i2);
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process, i1, i2, i3);
        if (WorkflowAction3.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process, i1, i2, i3);
        return ProcessContextProvider.super.call(process, i1, i2, i3);
    }

    @Override
    default <TProcess extends Process4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4) {
        if (i4 instanceof WorkflowOption option && WorkflowAction3.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflowWithOption(process, option, i1, i2, i3);
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process, i1, i2, i3, i4);
        if (WorkflowAction4.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process, i1, i2, i3, i4);
        return ProcessContextProvider.super.call(process, i1, i2, i3, i4);
    }

    @Override
    default <TProcess extends Process5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TProcess> process, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        if (i5 instanceof WorkflowOption option && WorkflowAction4.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflowWithOption(process, option, i1, i2, i3, i4);
        if (process.isAnnotationPresent(Activity.class) && isInWorkflowContext())
            return executeTask(process, i1, i2, i3, i4, i5);
        if (WorkflowAction5.class.isAssignableFrom(process) && isInWorkflowContext())
            return executeChildWorkflow(process, i1, i2, i3, i4, i5);
        return ProcessContextProvider.super.call(process, i1, i2, i3, i4, i5);
    }

    default <O> Promise<O> callAsync(Class<?> process) {
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process));
        if (WorkflowAction.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O, I> Promise<O> callAsync(Class<?> process, I i1) {
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process, i1));
        if (WorkflowAction1.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, i1);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O, I1, I2> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2) {
        if (i2 instanceof WorkflowOption option && WorkflowAction1.class.isAssignableFrom(process))
            return startChildWorkflowAsyncWithOption(process, option, i1);
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process, i1, i2));
        if (WorkflowAction2.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, i1, i2);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3) {
        if (i3 instanceof WorkflowOption option && WorkflowAction2.class.isAssignableFrom(process))
            return startChildWorkflowAsyncWithOption(process, option, i1, i2);
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process, i1, i2, i3));
        if (WorkflowAction3.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, i1, i2, i3);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3, I4> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3, I4 i4) {
        if (i4 instanceof WorkflowOption option && WorkflowAction3.class.isAssignableFrom(process))
            return startChildWorkflowAsyncWithOption(process, option, i1, i2, i3);
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process, i1, i2, i3, i4));
        if (WorkflowAction4.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, i1, i2, i3, i4);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3, I4, I5> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        if (i5 instanceof WorkflowOption option && WorkflowAction4.class.isAssignableFrom(process))
            return startChildWorkflowAsyncWithOption(process, option, i1, i2, i3, i4);
        if (process.isAnnotationPresent(Activity.class))
            return Async.function(() -> executeTask(process, i1, i2, i3, i4, i5));
        if (WorkflowAction5.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, i1, i2, i3, i4, i5);
        throw new IllegalArgumentException("Class must have @Activity annotation or extend WorkflowAction: " + process.getName());
    }

    default <O> Promise<O> callAsync(Class<?> process, ClosePolicy policy) {
        if (WorkflowAction.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy());
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <O, I> Promise<O> callAsync(Class<?> process, I i1, ClosePolicy policy) {
        if (WorkflowAction1.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy(), i1);
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <O, I1, I2> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, ClosePolicy policy) {
        if (WorkflowAction2.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy(), i1, i2);
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3, ClosePolicy policy) {
        if (WorkflowAction3.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy(), i1, i2, i3);
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3, I4> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3, I4 i4, ClosePolicy policy) {
        if (WorkflowAction4.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy(), i1, i2, i3, i4);
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <O, I1, I2, I3, I4, I5> Promise<O> callAsync(Class<?> process, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5, ClosePolicy policy) {
        if (WorkflowAction5.class.isAssignableFrom(process))
            return startChildWorkflowAsync(process, policy.toTemporalPolicy(), i1, i2, i3, i4, i5);
        throw new IllegalArgumentException("ChildPolicy only applies to WorkflowAction: " + process.getName());
    }

    default <T> List<T> await(List<Promise<T>> promises) {
        Promise.allOf(promises).get();
        return promises.stream().map(Promise::get).toList();
    }

    default <T> T await(Promise<T> promise) {
        return promise.get();
    }

    private boolean isInWorkflowContext() {
        try {
            Workflow.getInfo();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Object[] prependClassName(Class<?> clazz, Object... args) {
        return Stream.concat(Stream.of(clazz.getName()), Arrays.stream(args)).toArray();
    }

    private Object[] prependClassNameWithOption(Class<?> clazz, WorkflowOption option, Object... args) {
        return Stream.concat(Stream.concat(Stream.of(clazz.getName()), Arrays.stream(args)), Stream.of(option)).toArray();
    }

    private <O> O executeTask(Class<?> taskClass, Object... args) {
        var stub = Workflow.newUntypedActivityStub(buildActivityOptions(taskClass));
        Object result = stub.execute(taskClass.getSimpleName(), Object.class, prependClassName(taskClass, args));
        return TypeResolver.convert(result, getReturnType(taskClass));
    }

    private <O> Promise<O> startChildWorkflowAsync(Class<?> workflowClass, Object... args) {
        return startChildWorkflowAsync(workflowClass, ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON, args);
    }

    private <O> Promise<O> startChildWorkflowAsync(Class<?> workflowClass, ParentClosePolicy policy, Object... args) {
        ChildWorkflowStub stub = createChildWorkflowStub(workflowClass, policy);
        Promise<Object> resultPromise = stub.executeAsync(Object.class, prependClassName(workflowClass, args));
        stub.getExecution().get();
        return resultPromise.thenApply(result -> TypeResolver.convert(result, getReturnType(workflowClass)));
    }

    private <O> Promise<O> startChildWorkflowAsyncWithOption(Class<?> workflowClass, WorkflowOption option, Object... args) {
        ChildWorkflowStub stub = createChildWorkflowStub(workflowClass, ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON);
        Promise<Object> resultPromise = stub.executeAsync(Object.class, prependClassNameWithOption(workflowClass, option, args));
        stub.getExecution().get();
        return resultPromise.thenApply(result -> TypeResolver.convert(result, getReturnType(workflowClass)));
    }

    private <O> O executeChildWorkflow(Class<?> workflowClass, Object... args) {
        return executeChildWorkflow(workflowClass, ParentClosePolicy.PARENT_CLOSE_POLICY_TERMINATE, args);
    }

    private <O> O executeChildWorkflow(Class<?> workflowClass, ParentClosePolicy policy, Object... args) {
        ChildWorkflowStub stub = createChildWorkflowStub(workflowClass, policy);
        Object result = stub.execute(Object.class, prependClassName(workflowClass, args));
        return TypeResolver.convert(result, getReturnType(workflowClass));
    }

    private <O> O executeChildWorkflowWithOption(Class<?> workflowClass, WorkflowOption option, Object... args) {
        ChildWorkflowStub stub = createChildWorkflowStub(workflowClass, ParentClosePolicy.PARENT_CLOSE_POLICY_TERMINATE);
        Object result = stub.execute(Object.class, prependClassNameWithOption(workflowClass, option, args));
        return TypeResolver.convert(result, getReturnType(workflowClass));
    }

    private ChildWorkflowStub createChildWorkflowStub(Class<?> workflowClass, ParentClosePolicy parentClosePolicy) {
        var annotation = workflowClass.getAnnotation(net.trellisframework.workflow.temporal.annotation.Workflow.class);
        String defaultTaskQueue = Workflow.getInfo().getTaskQueue();
        String taskQueue = annotation != null && !annotation.taskQueue().isBlank() ? annotation.taskQueue() : defaultTaskQueue;
        ChildWorkflowOptions.Builder builder = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(workflowClass.getSimpleName() + "-" + Workflow.randomUUID())
                .setTaskQueue(taskQueue)
                .setParentClosePolicy(parentClosePolicy);
        Optional.ofNullable(annotation).map(net.trellisframework.workflow.temporal.annotation.Workflow::executionTimeout).map(StringUtil::nullIfBlank).map(DurationParser::parse).ifPresent(builder::setWorkflowExecutionTimeout);
        Optional.ofNullable(annotation).map(net.trellisframework.workflow.temporal.annotation.Workflow::taskTimeout).map(StringUtil::nullIfBlank).map(DurationParser::parse).ifPresent(builder::setWorkflowTaskTimeout);
        Optional.ofNullable(annotation).map(net.trellisframework.workflow.temporal.annotation.Workflow::runTimeout).map(StringUtil::nullIfBlank).map(DurationParser::parse).ifPresent(builder::setWorkflowRunTimeout);
         return Workflow.newUntypedChildWorkflowStub("DynamicWorkflowAction", builder.build());
    }

    private java.lang.reflect.Type getReturnType(Class<?> clazz) {
        for (java.lang.reflect.Type type : clazz.getGenericInterfaces()) {
            if (type instanceof java.lang.reflect.ParameterizedType pt) {
                java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                Class<?> rawClass = (Class<?>) pt.getRawType();
                int outputIndex = rawClass.getSimpleName().contains("Repository") ? 1 : 0;
                if (typeArgs.length > outputIndex) {
                    return typeArgs[outputIndex];
                }
            }
        }
        return Object.class;
    }

    private ActivityOptions buildActivityOptions(Class<?> activityClass) {
        Activity activity = activityClass.getAnnotation(Activity.class);
        ActivityOptions.Builder builder = ActivityOptions.newBuilder();
        if (activity != null) {
            Optional.ofNullable(activity.heartbeat()).ifPresent(x -> builder.setHeartbeatTimeout(DurationParser.parse(x)));
            Optional.ofNullable(StringUtil.nullIfBlank(activity.startToCloseTimeout())).ifPresent(x -> builder.setStartToCloseTimeout(DurationParser.parse(x)));
            Optional.ofNullable(StringUtil.nullIfBlank(activity.scheduleToStartTimeout())).ifPresent(x -> builder.setScheduleToStartTimeout(DurationParser.parse(x)));
            Optional.ofNullable(StringUtil.nullIfBlank(activity.scheduleToCloseTimeout())).ifPresent(x -> builder.setScheduleToCloseTimeout(DurationParser.parse(x)));
            RetryOptions.Builder retryBuilder = RetryOptions.newBuilder()
                    .setMaximumAttempts(activity.retry().maxAttempts())
                    .setInitialInterval(Duration.ofMillis(activity.retry().backoff().delay()))
                    .setMaximumInterval(Duration.ofMillis(activity.retry().backoff().maxDelay()))
                    .setBackoffCoefficient(activity.retry().backoff().multiplier());

            if (activity.retry().exclude().length > 0) {
                String[] exceptionNames = Arrays.stream(activity.retry().exclude()).map(Class::getName).toArray(String[]::new);
                retryBuilder.setDoNotRetry(exceptionNames);
            }
            builder.setRetryOptions(retryBuilder.build());
        } else {
            builder.setStartToCloseTimeout(DurationParser.parse(Activity.DEFAULT_START_TO_CLOSE_TIMEOUT));
            builder.setHeartbeatTimeout(DurationParser.parse(Activity.DEFAULT_HEARTBEAT));
        }
        return builder.build();
    }

}
