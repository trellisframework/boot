package net.trellisframework.context.task;

import net.trellisframework.context.process.Process4;

public abstract class Task4<TOutput, TInput1, TInput2, TInput3, TInput4>  extends BaseTask implements Process4<TOutput, TInput1, TInput2, TInput3, TInput4> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4);

}
