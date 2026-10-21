package net.trellisframework.workflow.temporal.util;

import net.trellisframework.workflow.temporal.workflow.DynamicWorkflowAction;

import java.util.List;


public final class WorkflowTypeResolver {


    private WorkflowTypeResolver() { }

    public static String forNewWorkflow(Class<?> workflowClass) {
        return configuredTypeName(workflowClass);
    }

    public static String forChildWorkflow(Class<?> workflowClass) {
        if (!useNamedWorkflowTypes())
            return legacyWorkflowType();
        return useConfiguredTypeName() ? configuredTypeName(workflowClass) : workflowClass.getName();
    }

    public static String forQueuedWorkflow(List<?> workArgs) {
        if (!useNamedWorkflowTypes() || workArgs.isEmpty() || !(workArgs.getFirst() instanceof String workflowClassName) || workflowClassName.isBlank())
            return legacyWorkflowType();
        try {
            Class<?> workflowClass = Class.forName(workflowClassName);
            return useConfiguredTypeName() ? configuredTypeName(workflowClass) : workflowClass.getName();
        } catch (ClassNotFoundException e) {
            return workflowClassName;
        }
    }

    private static String configuredTypeName(Class<?> workflowClass) {
        var annotation = workflowClass.getAnnotation(net.trellisframework.workflow.temporal.annotation.Workflow.class);
        return annotation == null || annotation.name().isBlank() ? workflowClass.getName() : annotation.name().trim();
    }

    private static boolean useNamedWorkflowTypes() {
        return WorkflowVersions.version(WorkflowVersions.WorkflowTypeResolver.WORKFLOW_TYPE_CHANGE_ID ,
                WorkflowVersions.WorkflowTypeResolver.NAMED_WORKFLOW_TYPES_VERSION) == WorkflowVersions.WorkflowTypeResolver.NAMED_WORKFLOW_TYPES_VERSION;
    }

    private static boolean useConfiguredTypeName() {
        return WorkflowVersions.version(WorkflowVersions.WorkflowTypeResolver.CUSTOM_WORKFLOW_TYPE_NAME_CHANGE_ID,
                WorkflowVersions.WorkflowTypeResolver.CUSTOM_WORKFLOW_TYPE_NAME_VERSION) == WorkflowVersions.WorkflowTypeResolver.CUSTOM_WORKFLOW_TYPE_NAME_VERSION;
    }


    private static String legacyWorkflowType() {
        return DynamicWorkflowAction.class.getSimpleName();
    }
}
