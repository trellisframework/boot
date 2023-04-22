package net.trellisframework.context.task;

import net.trellisframework.context.process.Process2;

public abstract class Task2<TOutput, TInput1, TInput2> extends BaseTask implements Process2<TOutput, TInput1, TInput2> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2);

}
