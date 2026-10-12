package net.trellisframework.workflow.temporal.logging;

import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.workflow.Workflow;
import net.trellisframework.core.log.Logger;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.workflow.temporal.failure.TemporalFailureDetails;
import net.trellisframework.workflow.temporal.failure.TemporalFailures;

public final class TemporalWorkerInterceptor extends WorkerInterceptorBase {

    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
        return new WorkflowInboundCallsInterceptorBase(next) {
            @Override
            public WorkflowOutput execute(WorkflowInput input) {
                try {
                    return super.execute(input);
                } catch (RuntimeException | Error failure) {
                    report(failure);
                    throw failure;
                }
            }
        };
    }

    private static void report(Throwable failure) {
        try {
            TemporalFailureDetails details = TemporalFailures.details(failure, Workflow.getInfo());
            Workflow.setCurrentDetails(JsonUtil.toString(details));
            Logger.error(details.workflowType(), "Workflow failed: " + details.exception(), failure);
        } catch (RuntimeException e) {
            failure.addSuppressed(e);
        }
    }
}
