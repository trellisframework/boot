package net.trellisframework.context.task;

import net.trellisframework.context.process.Process;

public abstract class Task<TOutput> extends BaseTask implements Process<TOutput> {

    public abstract TOutput execute();

}
