package net.trellisframework.context.process;


public interface Process2<O, I1, I2> extends BaseProcess {

    O execute(I1 i1, I2 i2);

}
