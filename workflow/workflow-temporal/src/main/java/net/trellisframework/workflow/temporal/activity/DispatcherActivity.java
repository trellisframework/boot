package net.trellisframework.workflow.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.workflow.temporal.config.WorkflowProperties;
import net.trellisframework.workflow.temporal.workflow.ConcurrencyDispatcherWorkflow;

import java.time.Duration;
import java.util.List;

@ActivityInterface
public interface DispatcherActivity {

    @ActivityMethod
    void enqueue(String concurrencyKey, int limit, int pageSize, String taskQueue, List<Object> workArgs);

    class Impl implements DispatcherActivity {
        @Override
        public void enqueue(String concurrencyKey, int limit, int pageSize, String taskQueue, List<Object> workArgs) {
            if (backend() == WorkflowProperties.ConcurrencyBackend.REDIS) {
                new DispatcherStore.Impl().enqueue(concurrencyKey, limit, taskQueue, workArgs);
                return;
            }

            String dispatcherId = "ConcurrencyDispatcher-" + concurrencyKey;
            WorkflowClient client = ApplicationContextProvider.context.getBean(WorkflowClient.class);
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(dispatcherId)
                    .setTaskQueue(taskQueue)
                    .setWorkflowTaskTimeout(Duration.ofSeconds(120))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
                    .build();
            WorkflowStub stub = client.newUntypedWorkflowStub("DynamicWorkflowAction", options);
            stub.signalWithStart(
                    "dispatch",
                    new Object[]{workArgs, limit},
                    new Object[]{ConcurrencyDispatcherWorkflow.CLASS_NAME, limit, pageSize});
        }

        private WorkflowProperties.ConcurrencyBackend backend() {
            try {
                return ApplicationContextProvider.context.getBean(WorkflowProperties.class).getConcurrencyBackend();
            } catch (Exception e) {
                return WorkflowProperties.ConcurrencyBackend.MEMORY;
            }
        }
    }

    static DispatcherActivity create() {
        return Workflow.newActivityStub(
            DispatcherActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToStartTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(10)
                    .setInitialInterval(Duration.ofMillis(200))
                    .setMaximumInterval(Duration.ofSeconds(5))
                    .build())
                .build()
        );
    }
}
