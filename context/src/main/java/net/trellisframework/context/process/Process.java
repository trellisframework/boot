package net.trellisframework.context.process;

public interface Process<TOutput> extends BaseProcess {
    TOutput execute ();
}
