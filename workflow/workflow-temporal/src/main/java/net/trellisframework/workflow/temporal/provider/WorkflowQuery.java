package net.trellisframework.workflow.temporal.provider;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.workflow.temporal.activity.DispatcherStore;
import net.trellisframework.workflow.temporal.config.WorkflowProperties;

public interface WorkflowQuery {

    default <T> T query(String workflowId, String queryType, Class<T> resultClass, Object... args) {
        WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
        WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
        return stub.query(queryType, resultClass, args);
    }

    default Object query(String workflowId, String queryType, Object... args) {
        return query(workflowId, queryType, Object.class, args);
    }

    default int getConcurrencyPendingCount(String concurrencyKey) {
        if (concurrencyBackend() == WorkflowProperties.ConcurrencyBackend.REDIS)
            return DispatcherStore.pendingCount(concurrencyKey);
        try {
            return query("ConcurrencyDispatcher-" + concurrencyKey, "getPendingCount", Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private WorkflowProperties.ConcurrencyBackend concurrencyBackend() {
        try {
            return ApplicationContextProvider.context.getBean(WorkflowProperties.class).getConcurrencyBackend();
        } catch (Exception e) {
            return WorkflowProperties.ConcurrencyBackend.MEMORY;
        }
    }

    default void updateConcurrencyLimit(String concurrencyKey, int newLimit) {
        try {
            WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
            WorkflowStub stub = client.newUntypedWorkflowStub("ConcurrencyDispatcher-" + concurrencyKey);
            stub.signal("updateLimit", newLimit);
        } catch (Exception ignored) {}
    }
}
