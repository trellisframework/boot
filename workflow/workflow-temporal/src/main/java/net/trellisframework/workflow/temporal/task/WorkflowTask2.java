package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process2;

public interface WorkflowTask2<O, I1, I2> extends BaseWorkflowTask, Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
