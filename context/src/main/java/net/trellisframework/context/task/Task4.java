package net.trellisframework.context.task;

import net.trellisframework.context.process.Process4;

public interface Task4<O, I1, I2, I3, I4> extends BaseTask, Process4<O, I1, I2, I3, I4> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4);

}
