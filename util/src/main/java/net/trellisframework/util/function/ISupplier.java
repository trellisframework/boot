package net.trellisframework.util.function;

@FunctionalInterface
public interface ISupplier<T> {

    T get() throws Exception;

}