package net.trellisframework.context.task;

import net.trellisframework.context.process.Process3;

public abstract class Task3<TOutput, TInput1, TInput2, TInput3> extends BaseTask implements Process3<TOutput, TInput1, TInput2, TInput3> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3);

}
