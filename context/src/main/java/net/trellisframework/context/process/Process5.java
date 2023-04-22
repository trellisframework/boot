package net.trellisframework.context.process;

public interface Process5<TOutput, TInput1, TInput2, TInput3, TInput4, TInput5> extends BaseProcess {

    TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3, TInput4 t4, TInput5 t5);

}
