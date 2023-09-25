package net.trellisframework.context.action;

import net.trellisframework.context.process.Process2;

public interface Action2<O, I1, I2> extends BaseAction , Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
