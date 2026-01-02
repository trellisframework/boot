package net.trellisframework.context.activity;

import net.trellisframework.context.process.Process3;

public interface Activity3<O, I1, I2, I3> extends BaseActivity, Process3<O, I1, I2, I3> {

    O execute(I1 i1, I2 i2, I3 i3);

}
