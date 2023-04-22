package net.trellisframework.context.rule;

public abstract class AbstractRule<T> {

    public abstract boolean condition(T t);

}
