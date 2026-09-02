package net.trellisframework.workflow.temporal.workflow;

import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.common.converter.EncodedValues;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.ChildWorkflowStub;
import io.temporal.workflow.DynamicSignalHandler;
import io.temporal.workflow.Workflow;
import net.trellisframework.core.log.Logger;
import net.trellisframework.workflow.temporal.activity.DispatcherStore;
import net.trellisframework.workflow.temporal.activity.DispatcherStore.Claim;
import net.trellisframework.workflow.temporal.util.ConcurrencyArgs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ConcurrencyTickerWorkflow implements DynamicSignalHandler {

    public static final String CLASS_NAME = ConcurrencyTickerWorkflow.class.getName();
    public static final String WORKFLOW_ID = "ConcurrencyTicker";
    public static final String SLOT_MARKER = "__TICKER_SLOT__";

    // Non-final & package-visible so tests can lower these to force the ContinueAsNew / idle-exit paths quickly.
    static int MAX_HISTORY = 5000;
    private static final int BATCH = 100;
    static int IDLE_TICKS_BEFORE_EXIT = 30;

    private int childCounter = 0;

    public void run(EncodedValues args) {
        DispatcherStore store = DispatcherStore.create();
        int idleTicks = 0;

        while (true) {
            List<Claim> claims = store.claim(BATCH);
            for (Claim claim : claims)
                startChild(claim);

            if (Workflow.getInfo().getHistoryLength() > MAX_HISTORY) {
                Workflow.continueAsNew(CLASS_NAME);
                return;
            }

            if (claims.isEmpty()) {
                if (++idleTicks >= IDLE_TICKS_BEFORE_EXIT)
                    return;
            } else {
                idleTicks = 0;
            }

            Workflow.sleep(Duration.ofSeconds(10));
        }
    }

    @Override
    public void handle(String signalName, EncodedValues args) {

    }

    private void startChild(Claim claim) {
        String idempotencyKey = ConcurrencyArgs.keyOf(claim.workArgs);
        List<Object> workArgs = ConcurrencyArgs.strip(claim.workArgs);

        String childId = idempotencyKey != null
                ? childName(workArgs) + "-" + idempotencyKey
                : childName(workArgs) + "-" + Workflow.currentTimeMillis() + "-" + (childCounter++);

        ChildWorkflowOptions opts = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(childId)
                .setTaskQueue(Workflow.getInfo().getTaskQueue())
                .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                .setWorkflowExecutionTimeout(Duration.ofHours(24))
                .build();

        ChildWorkflowStub stub = Workflow.newUntypedChildWorkflowStub("DynamicWorkflowAction", opts);
        List<Object> childArgs = new ArrayList<>(workArgs);
        childArgs.add(SLOT_MARKER);
        childArgs.add(claim.key);
        childArgs.add(claim.holderId);
        childArgs.add(claim.limit);
        try {
            stub.executeAsync(Object.class, childArgs.toArray());
            stub.getExecution().get();
        } catch (Exception e) {
            if (!isAlreadyStarted(e))
                throw e;
            Logger.warn("ConcurrencyTicker", "Skipped duplicate child already running: %s", childId);
        }
    }

    private boolean isAlreadyStarted(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName();
            if (name.contains("AlreadyStarted") || name.contains("AlreadyExists"))
                return true;
        }
        return false;
    }

    private String childName(List<Object> workArgs) {
        if (!workArgs.isEmpty() && workArgs.get(0) instanceof String className) {
            int dot = className.lastIndexOf('.');
            return dot >= 0 ? className.substring(dot + 1) : className;
        }
        return "child";
    }
}
