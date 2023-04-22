package net.trellisframework.context.action;

import net.trellisframework.context.process.Process;

public abstract class Action<TOutput> extends BaseAction implements Process<TOutput> {

    public abstract TOutput execute();

}
