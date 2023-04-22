package net.trellisframework.context.task;

import net.trellisframework.context.process.Process1;

public abstract class Task1<TOutput, TInput> extends BaseTask implements Process1<TOutput, TInput> {

    public abstract TOutput execute(TInput t1);

}
