package net.trellisframework.context.action;

import net.trellisframework.context.process.Process4;

public abstract class Action4<TOutput, TInput1, TInput2, TInput3, TInput4> extends BaseAction implements Process4<TOutput, TInput1, TInput2, TInput3, TInput4> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4);
}
