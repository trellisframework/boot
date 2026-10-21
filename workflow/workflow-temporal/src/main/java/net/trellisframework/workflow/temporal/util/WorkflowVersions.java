package net.trellisframework.workflow.temporal.util;

import io.temporal.workflow.Workflow;

public final class WorkflowVersions {

    private WorkflowVersions(){}

    public static class WorkflowInterceptor{
        public static final String USER_SEARCH_ATTRIBUTES_CHANGE_ID = "trellis-user-search-attributes";
        public static final int USER_SEARCH_ATTRIBUTES_VERSION = 1;
    }

    public static class WorkflowTypeResolver{
        public static final String WORKFLOW_TYPE_CHANGE_ID = "trellis-named-workflow-types";
        public static final int NAMED_WORKFLOW_TYPES_VERSION = 1;
        public static final String CUSTOM_WORKFLOW_TYPE_NAME_CHANGE_ID = "trellis-custom-workflow-type-name";
        public static final int CUSTOM_WORKFLOW_TYPE_NAME_VERSION = 1;

    }

    public static int version(String changeId, int maxVersion) {
        int version = Workflow.getVersion(changeId, Workflow.DEFAULT_VERSION, maxVersion);
        VersionArgs.upsertTypedSearchAttributes(changeId, maxVersion);
        return version;
    }
}
