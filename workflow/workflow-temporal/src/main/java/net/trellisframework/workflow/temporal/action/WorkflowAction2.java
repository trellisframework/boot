package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action2;

public interface WorkflowAction2<O, I1, I2> extends BaseWorkflowAction, Action2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
