package net.trellisframework.context.activity;

import net.trellisframework.context.process.Process2;

public interface Activity2<O, I1, I2> extends BaseActivity, Process2<O, I1, I2> {

    O execute(I1 i1, I2 i2);

}
