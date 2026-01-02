package net.trellisframework.context.workflow;

import net.trellisframework.context.process.Process;

public interface Workflow<O> extends BaseWorkflow, Process<O> {

    O execute();

}
