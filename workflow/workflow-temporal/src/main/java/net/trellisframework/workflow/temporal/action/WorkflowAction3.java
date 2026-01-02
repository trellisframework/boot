package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action3;

public interface WorkflowAction3<O, I1, I2, I3> extends BaseWorkflowAction, Action3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}
