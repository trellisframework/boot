package net.trellisframework.context.action;

import net.trellisframework.context.process.Process1;

public abstract class Action1<TOutput, TInput> extends BaseAction implements Process1<TOutput, TInput> {

    public abstract TOutput execute(TInput param);

}
