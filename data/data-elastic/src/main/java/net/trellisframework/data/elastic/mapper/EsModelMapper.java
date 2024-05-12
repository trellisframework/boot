package net.trellisframework.data.elastic.mapper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import net.trellisframework.data.core.util.PagingModelMapper;
import net.trellisframework.data.elastic.model.CoreDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface EsModelMapper extends PagingModelMapper {

    default <T extends CoreDocument> T plainToClass(Hit<T> hit) {
        T response = hit.source();
        Optional.ofNullable(response).ifPresent(x -> x.setId(hit.id()));
        return response;
    }

    default <T extends CoreDocument> List<T> plainToClass(SearchResponse<T> data) {
        return Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::hits).orElse(new ArrayList<>()).stream().map(this::plainToClass).toList();
    }

    default <T extends CoreDocument> Page<T> plainToClass(SearchResponse<T> data, Pageable pageable) {
        return new PageImpl<>(plainToClass(data),
                pageable,
                Optional.ofNullable(data).map(ResponseBody::hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L));
    }
}