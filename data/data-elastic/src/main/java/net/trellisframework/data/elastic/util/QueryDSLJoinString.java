package net.trellisframework.data.elastic.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

public interface QueryDSLJoinString {
    default String and(Set<String> values) {
        return join(values, " and ");
    }

    default String or(Set<String> values) {
        return join(values, " or ");
    }

    default String join(Set<String> values, CharSequence delimiter) {
        return values.stream().map(x -> StringUtils.wrap(StringUtils.remove(x, '"'), '"')).collect(Collectors.joining(delimiter));
    }
}
