package net.trellisframework.stream.event;

import net.trellisframework.context.provider.ActionContextProvider;
import net.trellisframework.util.mapper.ModelMapper;

import java.util.function.Function;

public interface StreamEvent<TOutput, TInput> extends Function<TInput, TOutput>, ModelMapper, ActionContextProvider {

    TOutput execute(TInput request);

    @Override
    default TOutput apply(TInput t) {
        return execute(t);
    }
}
