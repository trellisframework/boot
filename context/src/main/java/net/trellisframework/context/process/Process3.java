package net.trellisframework.context.process;

public interface Process3<TOutput, TInput1, TInput2, TInput3> extends BaseProcess {

    TOutput execute(TInput1 t1, TInput2 t2, TInput3 t3);

}
