package net.trellisframework.context.action;

import net.trellisframework.context.process.Process2;

public abstract class Action2<TOutput, TInput1, TInput2> extends BaseAction implements Process2<TOutput, TInput1, TInput2> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2);

}
