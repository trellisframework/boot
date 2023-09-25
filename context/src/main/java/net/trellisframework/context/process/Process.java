package net.trellisframework.context.process;

public interface Process<O> extends BaseProcess {
    O execute ();
}
