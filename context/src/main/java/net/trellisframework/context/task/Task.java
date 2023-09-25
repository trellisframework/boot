package net.trellisframework.context.task;

import net.trellisframework.context.process.Process;

public interface Task<O> extends BaseTask, Process<O> {

    O execute();

}
