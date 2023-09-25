package net.trellisframework.context.process;

public interface Process3<O, I1, I2, I3> extends BaseProcess {

    O execute(I1 i1, I2 i2, I3 i3);

}
