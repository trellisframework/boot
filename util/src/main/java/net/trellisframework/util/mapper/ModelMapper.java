package net.trellisframework.util.mapper;

import net.trellisframework.util.json.JsonUtil;
import org.dozer.DozerBeanMapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.TypeMappingOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface ModelMapper {

    default <S, D> DozerBeanMapper getModelMapper(Class<S> source, Class<D> destination, MappingOption... options) {
        DozerBeanMapper model_mapper = new DozerBeanMapper();
        model_mapper.addMapping(new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(source, destination, Arrays.stream(options).map(MappingOption::getMapping).toArray(TypeMappingOption[]::new));
            }
        });
        return model_mapper;
    }

    default <D> D plainToClass(Object source, Class<D> destination, MappingOption... options) {
        return getModelMapper(source.getClass(), destination, options).map(source, destination);
    }

    default <S, D> D plainToClass(S source, D destination, MappingOption... options) {
        getModelMapper(source.getClass(), destination.getClass(), options).map(source, destination);
        return destination;
    }

    default <S, D> List<D> plainToClass(Collection<S> source, final Class<D> destination, MappingOption... options) {
        final List<D> result = new ArrayList<>();
        if (source == null)
            return result;
        for (S element : source) {
            try {
                result.add(plainToClass(element, destination, options));
            } catch (Exception e) {
                result.add(JsonUtil.toObject(element, destination));
            }
        }
        return result;
    }
}
