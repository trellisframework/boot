package net.trellisframework.context.action;

import net.trellisframework.context.process.Process5;

public abstract class Action5<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5> extends BaseAction implements Process5<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4, TInput5 t5);
}
