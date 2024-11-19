package net.trellisframework.data.elastic.mapper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import net.trellisframework.data.core.util.PagingModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface EsModelMapper extends PagingModelMapper {

    default <T> T plainToClass(Hit<T> hit) {
        return hit.source();
    }

    default <T> List<T> plainToClass(SearchResponse<T> data) {
        return Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::hits).orElse(new ArrayList<>()).stream().map(this::plainToClass).toList();
    }

    default <T> Page<T> plainToClass(SearchResponse<T> data, Pageable pageable) {
        return new PageImpl<>(plainToClass(data),
                pageable,
                Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L));
    }
}