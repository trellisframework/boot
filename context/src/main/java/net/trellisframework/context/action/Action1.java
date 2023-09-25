package net.trellisframework.context.action;

import net.trellisframework.context.process.Process1;

public interface Action1<O, I> extends BaseAction , Process1<O, I> {

    O execute(I i1);

}
