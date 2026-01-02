package net.trellisframework.context.workflow;

import net.trellisframework.context.process.Process1;

public interface Workflow1<O, I> extends BaseWorkflow, Process1<O, I> {

    O execute(I i1);

}
