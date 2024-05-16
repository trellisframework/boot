package net.trellisframework.util.bool;

import lombok.AllArgsConstructor;

import java.util.function.Predicate;

@AllArgsConstructor(staticName = "of")
public class Bool {

    private final boolean value;

    public Bool(Predicate<Bool> when) {
        this.value = when.test(this);
    }

    public static Bool of(String value) {
        return of("true".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value));
    }

    public static Bool of(Predicate<Bool> when) {
        return new Bool(when);
    }

    public void ifTrue(Runnable then) {
        if (value)
            then.run();
    }

    public void ifFalse(Runnable then) {
        if (!value)
            then.run();
    }

    public void ifTrueOrElse(Runnable thenTrue, Runnable thenFalse) {
        if (value)
            thenTrue.run();
        else thenFalse.run();

    }

}
