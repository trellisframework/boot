package net.trellisframework.context.activity;

import net.trellisframework.context.process.Process;

public interface Activity<O> extends BaseActivity, Process<O> {

    O execute();

}
