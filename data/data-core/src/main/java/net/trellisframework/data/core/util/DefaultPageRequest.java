package net.trellisframework.data.core.util;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public class DefaultPageRequest extends PageRequest  {

    protected DefaultPageRequest(int page, int size, Sort sort) {
        super(page, size, sort);
    }

    public static PageRequest of(int page, int size) {
        return of(page, size, Sort.by(Sort.Direction.DESC, "created"));
    }

    public static PageRequest of(int page, int size, List<String> sort) {
        if (ObjectUtils.isEmpty(sort))
            return of(page, size);
        return PageRequest.of(page, size, Sort.by(sort.stream().map(x -> {
            String[] data = x.split(":");
            return new Sort.Order(Sort.Direction.fromOptionalString(data.length > 1 ? data[1] : "DESC").orElse(Sort.Direction.DESC), Optional.ofNullable(data[0]).orElse("created"));
        }).toList()));
    }
}
