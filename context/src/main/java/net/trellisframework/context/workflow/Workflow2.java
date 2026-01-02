package net.trellisframework.context.workflow;

import net.trellisframework.context.process.Process2;

public interface Workflow2<O, I1, I2> extends BaseWorkflow, Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
