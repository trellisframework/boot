package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process;

public interface WorkflowTask<O> extends BaseWorkflowTask, Process<O> {

    O execute();

}
