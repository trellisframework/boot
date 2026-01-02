package net.trellisframework.context.workflow;

import net.trellisframework.context.process.Process3;

public interface Workflow3<O, I1, I2, I3> extends BaseWorkflow, Process3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}
