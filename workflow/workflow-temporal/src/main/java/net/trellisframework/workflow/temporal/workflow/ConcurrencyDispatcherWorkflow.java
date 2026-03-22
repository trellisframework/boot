package net.trellisframework.workflow.temporal.workflow;

import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.ChildWorkflowStub;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.*;

public class ConcurrencyDispatcherWorkflow implements DynamicSignalHandler {

    public static final String CLASS_NAME = ConcurrencyDispatcherWorkflow.class.getName();
    private static final int MAX_HISTORY = 5000;

    private final Queue<List<Object>> queue = new LinkedList<>();
    private final Set<String> activeChildren = new HashSet<>();
    private int limit = 3;
    private int pageSize = 50;
    private int childCounter = 0;

    public void run(EncodedValues args) {
        this.limit = args.get(1, int.class);
        this.pageSize = args.get(2, int.class);

        List<List<Object>> initialQueue = null;
        if (args.getSize() > 3) {
            try { initialQueue = args.get(3, List.class); } catch (Exception ignored) {}
        }
        if (initialQueue != null)
            queue.addAll(initialQueue);

        List<String> previousChildren = null;
        if (args.getSize() > 4) {
            try { previousChildren = args.get(4, List.class); } catch (Exception ignored) {}
        }
        if (previousChildren != null)
            activeChildren.addAll(previousChildren);

        int idleCount = 0;

        while (true) {
            while (!queue.isEmpty() && activeChildren.size() < limit) {
                startChild(queue.poll());
                idleCount = 0;
            }

            if (Workflow.getInfo().getHistoryLength() > MAX_HISTORY) {
                Workflow.continueAsNew(CLASS_NAME, limit, pageSize,
                        new ArrayList<>(queue), new ArrayList<>(activeChildren));
                return;
            }

            if (queue.isEmpty() && activeChildren.isEmpty()) {
                idleCount++;
                if (idleCount >= 30)
                    return;
            } else {
                idleCount = 0;
            }

            Workflow.sleep(Duration.ofSeconds(10));
        }
    }

    @Override
    public void handle(String signalName, EncodedValues args) {
        switch (signalName) {
            case "dispatch" -> {
                List<Object> workArgs = args.get(0, List.class);
                queue.add(workArgs);
                try {
                    int newLimit = args.get(1, int.class);
                    if (newLimit > 0) limit = newLimit;
                } catch (Exception ignored) {}
            }
            case "reportCompletion" -> {
                String childWorkflowId = args.get(0, String.class);
                activeChildren.remove(childWorkflowId);
            }
        }
    }

    public Object handleQuery(String queryType, EncodedValues args) {
        if ("getPendingCount".equals(queryType)) {
            return queue.size() + activeChildren.size();
        }
        if ("getLimit".equals(queryType)) {
            return limit;
        }
        throw new IllegalArgumentException("Unknown query type: " + queryType);
    }

    private void startChild(List<Object> workArgs) {
        String childId = extractChildId(workArgs) + "-" + Workflow.currentTimeMillis() + "-" + (childCounter++);
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
    }

    private String extractChildId(List<Object> workArgs) {
        if (!workArgs.isEmpty() && workArgs.get(0) instanceof String className) {
            int dot = className.lastIndexOf('.');
            return dot >= 0 ? className.substring(dot + 1) : className;
        }
        return "child";
    }
}
