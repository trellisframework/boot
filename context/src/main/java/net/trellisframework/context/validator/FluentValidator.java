package net.trellisframework.context.validator;

import net.trellisframework.context.provider.ProcessContextProvider;
import net.trellisframework.http.exception.HttpException;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface FluentValidator<T> extends ProcessContextProvider {

    void execute();

    default FluentValidator<T> addRule(Predicate<? super T> when, Consumer<? super T> then) {
        if (when.test((T) this))
            then.accept((T) this);
        return this;
    }

    default FluentValidator<T> addRule(Consumer<? super T> then) {
        then.accept((T) this);
        return this;
    }

    default <X extends HttpException> FluentValidator<T> addRule(Predicate<? super T> when, Supplier<? extends X> exception) {
        if (when.test((T) this))
            throw exception.get();
        return this;
    }

}
