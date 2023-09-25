package net.trellisframework.context.action;

import net.trellisframework.context.process.Process5;

public interface Action5<O, I1, I2, I3, I4, I5> extends BaseAction , Process5<O, I1, I2, I3, I4, I5> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4, I5 i5);
}
