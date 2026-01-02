package net.trellisframework.workflow.temporal.action;

import net.trellisframework.context.action.Action;

public interface WorkflowAction<O> extends BaseWorkflowAction, Action<O> {

    O execute();

}
