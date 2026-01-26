package net.trellisframework.workflow.temporal.workflow;

import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.DynamicQueryHandler;
import io.temporal.workflow.DynamicWorkflow;
import io.temporal.workflow.Workflow;
import net.trellisframework.context.action.*;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.workflow.temporal.action.Queryable;
import net.trellisframework.workflow.temporal.util.TypeResolver;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DynamicWorkflowAction implements DynamicWorkflow, DynamicQueryHandler {

    public static final String SEARCH_ATTR_CONCURRENCY_KEY = "ConcurrencyKey";

    private Object workflowBean;
    private String workflowClassName;

    @Override
    public Object execute(EncodedValues args) {
        workflowClassName = args.get(0, String.class);
        try {
            Class<?> workflowClass = Class.forName(workflowClassName);
            workflowBean = ApplicationContextProvider.context.getBean(workflowClass);

            if (workflowBean instanceof Queryable) {
                Workflow.registerListener(this);
            }

            Class<?>[] paramTypes = TypeResolver.getParameterTypes(workflowClass, BaseAction.class);
            return executeWorkflow(workflowBean, args, paramTypes);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Workflow class not found: " + workflowClassName, e);
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
