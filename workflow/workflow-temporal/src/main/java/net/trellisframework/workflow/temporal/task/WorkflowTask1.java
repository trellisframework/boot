package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process1;

public interface WorkflowTask1<O, I> extends BaseWorkflowTask, Process1<O, I> {

    O execute(I i1);

}
