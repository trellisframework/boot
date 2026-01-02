package net.trellisframework.workflow.temporal.task;

import net.trellisframework.context.process.Process5;

public interface WorkflowTask5<O, I1, I2, I3, I4, I5> extends BaseWorkflowTask, Process5<O, I1, I2, I3, I4, I5> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4, I5 i5);

}
