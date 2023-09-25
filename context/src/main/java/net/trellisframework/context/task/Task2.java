package net.trellisframework.context.task;

import net.trellisframework.context.process.Process2;

public interface Task2<O, I1, I2> extends BaseTask, Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
