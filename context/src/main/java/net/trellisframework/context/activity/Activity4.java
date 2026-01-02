package net.trellisframework.context.activity;

import net.trellisframework.context.process.Process4;

public interface Activity4<O, I1, I2, I3, I4> extends BaseActivity, Process4<O, I1, I2, I3, I4> {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4);

}
