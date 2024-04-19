package net.trellisframework.data.elastic.mapper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import net.trellisframework.data.core.util.PagingModelMapper;
import net.trellisframework.data.elastic.payload.EsPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Optional;

public interface EsModelMapper extends PagingModelMapper {

    default <T extends EsPayload> T plainToClass(Hit<T> hit) {
        T response = hit.source();
        Optional.ofNullable(response).ifPresent(x -> x.setId(hit.id()));
        return response;
    }

    default <T extends EsPayload> Page<T> plainToClass(SearchResponse<T> data, Pageable pageable) {
        return new PageImpl<>(Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::hits).orElse(new ArrayList<>()).stream().map(this::plainToClass).toList(),
                pageable,
                Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L));
    }
}