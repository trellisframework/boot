package net.trellisframework.context.process;

public interface Process1<TOutput, TInput> extends BaseProcess {

    TOutput execute(TInput param);

}
