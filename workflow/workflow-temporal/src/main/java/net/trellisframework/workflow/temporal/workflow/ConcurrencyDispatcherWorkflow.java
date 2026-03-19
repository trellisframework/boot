package net.trellisframework.workflow.temporal.workflow;

import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.workflow.*;

import java.time.Duration;
import java.util.*;

@WorkflowInterface
public interface ConcurrencyDispatcherWorkflow {

    @WorkflowMethod
    void run(int limit, int pageSize, List<List<Object>> initialQueue);

    @SignalMethod
    void dispatch(List<Object> workArgs);

    @SignalMethod
    void reportCompletion(String childWorkflowId);

    @QueryMethod
    int getPendingCount();

    class Impl implements ConcurrencyDispatcherWorkflow {

        private final Queue<List<Object>> queue = new LinkedList<>();
        private final Set<String> activeChildren = new HashSet<>();
        private int limit = 3;
        private int pageSize = 50;
        private int startedInThisRun = 0;

        @Override
        public void run(int limit, int pageSize, List<List<Object>> initialQueue) {
            this.limit = limit;
            this.pageSize = pageSize;
            if (initialQueue != null)
                initialQueue.forEach(queue::add);

            while (true) {
                boolean changed = Workflow.await(Duration.ofMinutes(5),
                        () -> !queue.isEmpty() || activeChildren.isEmpty() || shouldContinueAsNew());

                if (!changed && queue.isEmpty() && activeChildren.isEmpty())
                    return;

                if (shouldContinueAsNew()) {
                    Workflow.newContinueAsNewStub(ConcurrencyDispatcherWorkflow.class)
                            .run(limit, pageSize, new ArrayList<>(queue));
                    return;
                }

                while (!queue.isEmpty() && activeChildren.size() < limit) {
                    startChild(queue.poll());
                }
            }
        }

        @Override
        public void dispatch(List<Object> workArgs) {
            queue.add(workArgs);
        }

        @Override
        public void reportCompletion(String childWorkflowId) {
            activeChildren.remove(childWorkflowId);
        }

        @Override
        public int getPendingCount() {
            return queue.size() + activeChildren.size();
        }

        private void startChild(List<Object> workArgs) {
            String childId = extractChildId(workArgs) + "-" + Workflow.randomUUID();
            String dispatcherId = Workflow.getInfo().getWorkflowId();

            ChildWorkflowOptions opts = ChildWorkflowOptions.newBuilder()
                    .setWorkflowId(childId)
                    .setTaskQueue(Workflow.getInfo().getTaskQueue())
                    .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                    .setWorkflowExecutionTimeout(Duration.ofHours(24))
                    .build();

            ChildWorkflowStub stub = Workflow.newUntypedChildWorkflowStub("DynamicWorkflowAction", opts);
            List<Object> argsWithDispatcher = new ArrayList<>(workArgs);
            argsWithDispatcher.add(dispatcherId);
            argsWithDispatcher.add(childId);
            stub.executeAsync(Object.class, argsWithDispatcher.toArray());
            stub.getExecution().get();
            activeChildren.add(childId);
            startedInThisRun++;
        }

        private boolean shouldContinueAsNew() {
            return startedInThisRun >= pageSize && activeChildren.isEmpty() && !queue.isEmpty();
        }

        private String extractChildId(List<Object> workArgs) {
            if (!workArgs.isEmpty() && workArgs.get(0) instanceof String className) {
                int dot = className.lastIndexOf('.');
                return dot >= 0 ? className.substring(dot + 1) : className;
            }
            return "child";
        }
    }
}
