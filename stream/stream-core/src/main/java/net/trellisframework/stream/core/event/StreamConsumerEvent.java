package net.trellisframework.stream.core.event;

import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.mapper.ModelMapper;

import java.util.function.Consumer;

public interface StreamConsumerEvent<I> extends Consumer<I>, ModelMapper, ActionContextProvider {

    void execute(I request);

    @Override
    default void accept(I i) {
        execute(i);
    }
}
