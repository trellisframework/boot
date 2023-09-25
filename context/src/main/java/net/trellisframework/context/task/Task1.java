package net.trellisframework.context.task;

import net.trellisframework.context.process.Process1;

public interface Task1<O, I> extends BaseTask, Process1<O, I> {

    O execute(I i1);

}
