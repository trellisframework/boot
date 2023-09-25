package net.trellisframework.data.core.util;

import net.trellisframework.util.mapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.stream.Collectors;

public interface PagingModelMapper extends ModelMapper {

    default <S, D> Page<D> plainToClass(Page<S> source, final Class<D> destination) {
        List<D> contents = source.getContent().stream().map(x -> plainToClass(x, destination)).collect(Collectors.toList());
        return new PageImpl<>(contents, source.getPageable(), source.getTotalElements());
    }

}
