package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action4;

public interface WorkflowAction4<O, I1, I2, I3, I4> extends BaseWorkflowAction, Action4<O, I1, I2, I3, I4> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4);

}
