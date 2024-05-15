package net.trellisframework.util.bool;

import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor(staticName = "of")
public class Bool {

    private final boolean value;

    public void ifTrue(Consumer<Bool> action) {
        if (value)
            action.accept(this);
    }

    public void ifFalse(Consumer<Bool> action) {
        if (!value)
            action.accept(this);
    }

    public void ifTrueOrElse(Consumer<Bool> trueConsumer, Consumer<Bool> falseConsumer) {
        if (value)
            trueConsumer.accept(this);
        else falseConsumer.accept(this);

    }

}
