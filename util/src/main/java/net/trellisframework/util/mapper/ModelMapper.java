package net.trellisframework.util.mapper;

import com.fasterxml.jackson.databind.*;
import net.trellisframework.http.exception.BadRequestException;
import net.trellisframework.util.constant.Messages;
import net.trellisframework.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ModelMapper {

    default <S, D> ObjectMapper getModelMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        return mapper;
    }

    default <D> D plainToClass(Object source, Class<D> destination) {
        return getModelMapper().convertValue(source, destination);
    }

    default <S, D> D plainToClass(S source, D destination) {
        try {
            ObjectMapper mapper = getModelMapper();
            return mapper.readerForUpdating(destination).readValue(mapper.writeValueAsString(source));
        } catch (Exception e) {
            throw new BadRequestException(Messages.CAN_NOT_DESERIALIZE_OBJECT);
        }
    }

    default <S, D> List<D> plainToClass(Collection<S> source, final Class<D> destination) {
        final List<D> result = new ArrayList<>();
        if (source == null)
            return result;
        for (S element : source) {
            try {
                result.add(plainToClass(element, destination));
            } catch (Exception e) {
                result.add(JsonUtil.toObject(element, destination));
            }
        }
        return result;
    }
}
