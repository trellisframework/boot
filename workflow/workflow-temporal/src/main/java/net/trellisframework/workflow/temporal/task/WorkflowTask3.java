package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process3;

public interface WorkflowTask3<O, I1, I2, I3> extends BaseWorkflowTask, Process3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}
