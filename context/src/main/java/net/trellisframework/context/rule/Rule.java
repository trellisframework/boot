package net.trellisframework.context.rule;

import net.trellisframework.http.exception.HttpException;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Rule<T> {

    private final T value;

    private Rule(T value) {
        this.value = value;
    }

    public static <T> Rule<T> on(T value) {
        return new Rule<>(value);
    }

    public Rule<T> add(Predicate<? super T> when, Consumer<? super T> then) {
        if (when.test(value))
            then.accept(value);
        return this;
    }

    public <X extends HttpException> Rule<T> add(Predicate<? super T> when, Supplier<? extends X> exception) {
        if (when.test(value))
            throw exception.get();
        return this;
    }
}