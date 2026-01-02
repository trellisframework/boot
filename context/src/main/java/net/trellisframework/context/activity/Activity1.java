package net.trellisframework.context.activity;

import net.trellisframework.context.process.Process1;

public interface Activity1<O, I> extends BaseActivity, Process1<O, I> {

    O execute(I i1);

}
