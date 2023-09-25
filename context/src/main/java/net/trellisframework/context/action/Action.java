package net.trellisframework.context.action;

import net.trellisframework.context.process.Process;

public interface Action<O> extends BaseAction, Process<O> {

    O execute();

}
