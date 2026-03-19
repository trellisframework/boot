package net.trellisframework.workflow.temporal.provider;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import net.trellisframework.core.application.ApplicationContextProvider;

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
        try {
            return query("ConcurrencyDispatcher-" + concurrencyKey, "getPendingCount", Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }
}
