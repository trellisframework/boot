package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action5;

public interface WorkflowAction5<O, I1, I2, I3, I4, I5> extends BaseWorkflowAction, Action5<O, I1, I2, I3, I4, I5> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4, I5 i5);

}
