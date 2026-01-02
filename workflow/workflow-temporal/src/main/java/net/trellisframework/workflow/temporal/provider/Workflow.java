package net.trellisframework.workflow.temporal.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.client.WorkflowStub;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import net.trellisframework.context.action.*;
import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.redis.semaphore.RedisSemaphore;
import net.trellisframework.workflow.temporal.action.*;
import net.trellisframework.workflow.temporal.annotation.Async;
import net.trellisframework.workflow.temporal.config.WorkflowProperties;
import net.trellisframework.util.duration.DurationParser;
import net.trellisframework.workflow.temporal.payload.Fallback;
import net.trellisframework.workflow.temporal.payload.WorkflowOption;
import net.trellisframework.workflow.temporal.workflow.DynamicWorkflowAction;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unchecked")
public interface Workflow extends ActionContextProvider {

    @Override
    default <A extends Action<O>, O> O call(Class<A> action) {
        if (WorkflowAction.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action));
        }
        return ActionContextProvider.super.call(action);
    }

    @Override
    default <A extends Action1<O, I1>, O, I1> O call(Class<A> action, I1 i1) {
        if (WorkflowAction1.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action), i1);
        }
        return ActionContextProvider.super.call(action, i1);
    }

    @Override
    default <A extends Action2<O, I1, I2>, O, I1, I2> O call(Class<A> action, I1 i1, I2 i2) {
        if (WorkflowAction2.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action), i1, i2);
        }
        return ActionContextProvider.super.call(action, i1, i2);
    }

    @Override
    default <A extends Action3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<A> action, I1 i1, I2 i2, I3 i3) {
        if (WorkflowAction3.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action), i1, i2, i3);
        }
        return ActionContextProvider.super.call(action, i1, i2, i3);
    }

    @Override
    default <A extends Action4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<A> action, I1 i1, I2 i2, I3 i3, I4 i4) {
        if (WorkflowAction4.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action), i1, i2, i3, i4);
        }
        return ActionContextProvider.super.call(action, i1, i2, i3, i4);
    }

    @Override
    default <A extends Action5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<A> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5) {
        if (WorkflowAction5.class.isAssignableFrom(action)) {
            return doCall(action, null, null, isAsync(action), i1, i2, i3, i4, i5);
        }
        return ActionContextProvider.super.call(action, i1, i2, i3, i4, i5);
    }

    default <TAction extends WorkflowAction<O>, O> O call(Class<TAction> action, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action));
    }

    default <TAction extends WorkflowAction<O>, O> O call(Class<TAction> action, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action));
    }

    default <TAction extends WorkflowAction<O>, O> O call(Class<TAction> action, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action));
    }

    default <TAction extends WorkflowAction1<O, I>, O, I> O call(Class<TAction> action, I i1, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action), i1);
    }

    default <TAction extends WorkflowAction1<O, I>, O, I> O call(Class<TAction> action, I i1, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action), i1);
    }

    default <TAction extends WorkflowAction1<O, I>, O, I> O call(Class<TAction> action, I i1, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action), i1);
    }

    default <TAction extends WorkflowAction2<O, I1, I2>, O, I1, I2> O call(Class<TAction> action, I1 i1, I2 i2, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action), i1, i2);
    }

    default <TAction extends WorkflowAction2<O, I1, I2>, O, I1, I2> O call(Class<TAction> action, I1 i1, I2 i2, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action), i1, i2);
    }

    default <TAction extends WorkflowAction2<O, I1, I2>, O, I1, I2> O call(Class<TAction> action, I1 i1, I2 i2, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action), i1, i2);
    }

    default <TAction extends WorkflowAction3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action), i1, i2, i3);
    }

    default <TAction extends WorkflowAction3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action), i1, i2, i3);
    }

    default <TAction extends WorkflowAction3<O, I1, I2, I3>, O, I1, I2, I3> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action), i1, i2, i3);
    }

    default <TAction extends WorkflowAction4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action), i1, i2, i3, i4);
    }

    default <TAction extends WorkflowAction4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action), i1, i2, i3, i4);
    }

    default <TAction extends WorkflowAction4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action), i1, i2, i3, i4);
    }

    default <TAction extends WorkflowAction5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5, WorkflowOption<O> option) {
        return doCall(action, option, null, isAsync(action), i1, i2, i3, i4, i5);
    }

    default <TAction extends WorkflowAction5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5, Fallback<O> fallback) {
        return doCall(action, null, fallback, isAsync(action), i1, i2, i3, i4, i5);
    }

    default <TAction extends WorkflowAction5<O, I1, I2, I3, I4, I5>, O, I1, I2, I3, I4, I5> O call(Class<TAction> action, I1 i1, I2 i2, I3 i3, I4 i4, I5 i5, WorkflowOption<O> option, Fallback<O> fallback) {
        return doCall(action, option, fallback, isAsync(action), i1, i2, i3, i4, i5);
    }

    private boolean isAsync(Class<?> action) {
        return action.isAnnotationPresent(Async.class);
    }

    private boolean isFireAndForget(boolean async, Fallback<?> fallback) {
        if (async) return true;
        return fallback != null && fallback.getTimeout() != null && fallback.getTimeout().isZero();
    }

    private <O> O doCall(Class<?> action, WorkflowOption<O> option, Fallback<O> fallback, boolean async, Object... args) {
        WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
        WorkflowProperties properties = ApplicationContextProvider.context.getBean(WorkflowProperties.class);

        boolean fireAndForget = isFireAndForget(async, fallback);

        String concurrencyKey = (option != null && option.hasConcurrency()) ? option.getConcurrencyKey() : null;
        int concurrencyLimit = (option != null) ? option.getConcurrencyLimit() : 0;
        boolean acquired = false;

        try {
            if (concurrencyKey != null && !fireAndForget) {
                RedisSemaphore.acquire(concurrencyKey, concurrencyLimit);
                acquired = true;
            }
            WorkflowStub stub = createStub(client, properties, action, option);
            Object[] allArgs = prepareArgs(action, option, args);
            stub.start(allArgs);
            if (fireAndForget) {
                return fallback != null && fallback.hasValue() ? fallback.getValue().get() : null;
            }
            return waitForResult(stub, action, fallback);
        } finally {
            if (acquired) {
                RedisSemaphore.release(concurrencyKey, concurrencyLimit);
            }
        }
    }

    private <O> O waitForResult(WorkflowStub stub, Class<?> action, Fallback<O> fallback) {
        Duration timeout = fallback != null ? fallback.getTimeout() : null;
        if (timeout == null) {
            try {
                Object result = stub.getResult(Object.class);
                return convert(result, getReturnType(action));
            } catch (WorkflowException e) {
                throw unwrapException(e);
            }
        }
        if (fallback.hasValue()) {
            try {
                Object result = stub.getResult(timeout.toMillis(), TimeUnit.MILLISECONDS, Object.class);
                return convert(result, getReturnType(action));
            } catch (TimeoutException e) {
                return fallback.getValue().get();
            } catch (WorkflowException e) {
                throw unwrapException(e);
            }
        }
        return null;
    }

    private RuntimeException unwrapException(WorkflowException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ApplicationFailure af) {
                String exceptionType = af.getType();
                String message = af.getOriginalMessage();
                try {
                    Class<?> exceptionClass = Class.forName(exceptionType);
                    if (RuntimeException.class.isAssignableFrom(exceptionClass)) {
                        try {
                            return (RuntimeException) exceptionClass.getConstructor(String.class).newInstance(message);
                        } catch (Exception ex) {
                            try {
                                return (RuntimeException) exceptionClass.getConstructor().newInstance();
                            } catch (Exception ex2) {
                                return new RuntimeException(message);
                            }
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    return new RuntimeException(message);
                }
            }
            if (cause instanceof ActivityFailure || cause instanceof ChildWorkflowFailure) {
                cause = cause.getCause();
            } else {
                break;
            }
        }
        if (e.getCause() instanceof RuntimeException re) {
            return re;
        }
        return new RuntimeException(e.getMessage(), e);
    }

    private WorkflowStub createStub(WorkflowClient client, WorkflowProperties properties, Class<?> action, WorkflowOption<?> option) {
        String taskQueue = StringUtils.defaultIfBlank(properties.getTaskQueue(),
                ApplicationContextProvider.context.getEnvironment().getProperty("spring.application.name", "default"));

        var workflowAnnotation = action.getAnnotation(net.trellisframework.workflow.temporal.annotation.Workflow.class);
        Duration timeout = workflowAnnotation != null
                ? DurationParser.parse(workflowAnnotation.timeout())
                : Duration.ofHours(24);

        io.temporal.client.WorkflowOptions.Builder optionsBuilder = io.temporal.client.WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(action.getSimpleName() + "-" + UUID.randomUUID())
                .setWorkflowExecutionTimeout(timeout);

        if (option != null && option.hasPriority()) {
            optionsBuilder.setPriority(option.getPriority());
        }
        if (option != null && option.hasConcurrency()) {
            SearchAttributes searchAttributes = SearchAttributes.newBuilder()
                    .set(SearchAttributeKey.forKeyword(DynamicWorkflowAction.SEARCH_ATTR_CONCURRENCY_KEY), option.getConcurrencyKey())
                    .build();
            optionsBuilder.setTypedSearchAttributes(searchAttributes);
        }

        return client.newUntypedWorkflowStub("DynamicWorkflowAction", optionsBuilder.build());
    }

    private Object[] prepareArgs(Class<?> action, WorkflowOption<?> option, Object[] args) {
        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = action.getName();
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return allArgs;
    }

    private <O> O convert(Object result, Class<?> targetType) {
        if (result == null || targetType == Void.class || targetType == void.class) {
            return null;
        }
        if (targetType.isInstance(result)) {
            return (O) result;
        }
        ObjectMapper mapper = ApplicationContextProvider.context.getBean(ObjectMapper.class);
        return (O) mapper.convertValue(result, targetType);
    }

    private Class<?> getReturnType(Class<?> actionClass) {
        for (Type type : actionClass.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();
                if (rawType instanceof Class<?> rawClass && BaseWorkflowAction.class.isAssignableFrom(rawClass)) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
                        return (Class<?>) typeArgs[0];
                    }
                }
            }
        }
        return Object.class;
    }

    default <T> T query(String workflowId, String queryType, Class<T> resultClass, Object... args) {
        WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
        WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
        return stub.query(queryType, resultClass, args);
    }

    default Object query(String workflowId, String queryType, Object... args) {
        return query(workflowId, queryType, Object.class, args);
    }
}
