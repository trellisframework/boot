package net.trellisframework.workflow.temporal.workflow;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.*;
import net.trellisframework.context.action.*;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.workflow.temporal.action.Queryable;
import net.trellisframework.workflow.temporal.activity.DistributedLockActivity;
import net.trellisframework.workflow.temporal.payload.WorkflowOption;
import net.trellisframework.workflow.temporal.util.TypeResolver;

import java.time.Duration;
import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DynamicWorkflowAction implements DynamicWorkflow, DynamicQueryHandler {

    public static final String SEARCH_ATTR_CONCURRENCY_KEY = "ConcurrencyKey";
    private Object workflowBean;
    private String workflowClassName;
    private WorkflowOption workflowOption;

    @Override
    public Object execute(EncodedValues args) {
        workflowClassName = args.get(0, String.class);
        try {
            Class<?> workflowClass = Class.forName(workflowClassName);
            workflowBean = ApplicationContextProvider.context.getBean(workflowClass);

            if (workflowBean instanceof Queryable) {
                Workflow.registerListener(this);
            }

            workflowOption = extractWorkflowOption(args);
            Class<?>[] paramTypes = TypeResolver.getParameterTypes(workflowClass, BaseAction.class);

            String key = null;
            String holderId = null;
            DistributedLockActivity lockActivity = null;
            CancellationScope heartbeatScope = null;
            if (hasConcurrency()) {
                key = workflowOption.getConcurrencyKey();
                holderId = Workflow.getInfo().getWorkflowId();
                lockActivity = DistributedLockActivity.create();
                acquire(lockActivity, key, holderId, workflowOption.getConcurrencyLimit());
                
                final String lockKey = key;
                final String lockHolderId = holderId;
                final int lockLimit = workflowOption.getConcurrencyLimit();
                final DistributedLockActivity activity = lockActivity;
                
                heartbeatScope = Workflow.newDetachedCancellationScope(() -> {
                    while (true) {
                        Workflow.sleep(Duration.ofSeconds(DistributedLockActivity.RENEW_INTERVAL_SECONDS));
                        try {
                            activity.keepAlive(lockKey, lockHolderId, lockLimit);
                        } catch (Exception ignored) {
                        }
                    }
                });
                Async.procedure(heartbeatScope::run);
            }

            try {
                return executeWorkflow(workflowBean, args, paramTypes);
            } finally {
                if (heartbeatScope != null) {
                    heartbeatScope.cancel();
                }
                if (lockActivity != null) {
                    lockActivity.release(key, holderId);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Workflow class not found: " + workflowClassName, e);
        }
    }

    private WorkflowOption extractWorkflowOption(EncodedValues args) {
        if (args.getSize() > 1) {
            try {
                return args.get(args.getSize() - 1, WorkflowOption.class);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean hasConcurrency() {
        return Optional.ofNullable(workflowOption).map(WorkflowOption::hasConcurrency).orElse(false);
    }

    private void acquire(DistributedLockActivity activity, String key, String holderId, int limit) {
        while (!activity.tryAcquire(key, holderId, limit)) {
            Workflow.sleep(Duration.ofSeconds(1));
        }
    }

    private Object executeWorkflow(Object workflow, EncodedValues args, Class<?>[] paramTypes) {
        return switch (workflow) {
            case Action5 w -> w.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3), arg(args, 5, paramTypes, 4));
            case Action4 w -> w.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2), arg(args, 4, paramTypes, 3));
            case Action3 w -> w.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1), arg(args, 3, paramTypes, 2));
            case Action2 w -> w.execute(arg(args, 1, paramTypes, 0), arg(args, 2, paramTypes, 1));
            case Action1 w -> w.execute(arg(args, 1, paramTypes, 0));
            case Action<?> w -> w.execute();
            default -> throw new IllegalArgumentException("Unknown workflow type: " + workflowClassName);
        };
    }

    private Object arg(EncodedValues args, int argIndex, Class<?>[] paramTypes, int typeIndex) {
        return TypeResolver.convert(args.get(argIndex, Object.class), paramTypes[typeIndex]);
    }

    @Override
    public Object handle(String queryType, EncodedValues args) {
        if (workflowBean instanceof Queryable queryable) {
            Object[] queryArgs = new Object[args.getSize()];
            for (int i = 0; i < args.getSize(); i++) {
                queryArgs[i] = args.get(i, Object.class);
            }
            return queryable.query(queryType, queryArgs);
        }
        throw new IllegalStateException("Workflow does not implement Queryable: " + workflowClassName);
    }
}
