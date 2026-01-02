package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action1;

public interface WorkflowAction1<O, I> extends BaseWorkflowAction, Action1<O, I> {

    O execute(I i1);

}
