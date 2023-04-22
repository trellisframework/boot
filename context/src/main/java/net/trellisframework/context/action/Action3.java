package net.trellisframework.context.action;

import net.trellisframework.context.process.Process3;

public abstract class Action3<TOutput, TInput1, TInput2, TInput3> extends BaseAction implements Process3<TOutput, TInput1, TInput2, TInput3> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3);
}
