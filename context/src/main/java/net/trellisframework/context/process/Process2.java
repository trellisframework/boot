package net.trellisframework.context.process;


public interface Process2<TOutput, TInput1, TInput2> extends BaseProcess {

    TOutput execute(TInput1 t1, TInput2 t2);

}
