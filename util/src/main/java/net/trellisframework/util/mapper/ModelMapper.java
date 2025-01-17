package net.trellisframework.util.mapper;

import net.trellisframework.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ModelMapper {

    default <D> D plainToClass(Object source, Class<D> destination) {
        return JsonUtil.toObject(source, destination);
    }

    default <S, D> D plainToClass(S source, D destination) {
        return JsonUtil.toObject(source, destination);
    }

    default <S, D> List<D> plainToClass(Collection<S> source, final Class<D> destination) {
        final List<D> result = new ArrayList<>();
        if (source == null)
            return result;
        for (S element : source) {
            result.add(JsonUtil.toObject(element, destination));
        }
        return result;
    }
}
