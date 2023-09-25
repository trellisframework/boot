package net.trellisframework.context.process;

public interface Process4<O, I1, I2, I3, I4> extends BaseProcess {

    O execute(I1 i1, I2 i2, I3 i3, I4 i4);

}
