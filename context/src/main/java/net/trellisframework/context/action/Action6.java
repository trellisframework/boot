package net.trellisframework.context.action;

import net.trellisframework.context.process.Process6;

public abstract class Action6<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5, TInput6> extends BaseAction implements Process6<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5, TInput6> {

    public abstract TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4, TInput5 t5, TInput6 t6);
}
