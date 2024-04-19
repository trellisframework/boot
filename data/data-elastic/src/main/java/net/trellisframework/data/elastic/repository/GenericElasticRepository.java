package net.trellisframework.data.elastic.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.data.elastic.configuration.ElasticsearchConfig;
import net.trellisframework.data.elastic.mapper.EsModelMapper;
import net.trellisframework.data.elastic.payload.EsPayload;
import net.trellisframework.data.elastic.util.QueryDSLJoinString;
import net.trellisframework.http.exception.BadGatewayException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.IOException;
import java.util.function.Function;

@NoRepositoryBean
public interface GenericElasticRepository<TEntity, ID> extends GenericRepository, QueryDSLJoinString, EsModelMapper {

    default <T> SearchResponse<T> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function, Class<T> clazz) {
        try {
            return ElasticsearchConfig.getInstance().search(function, clazz);
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    default <T extends EsPayload> Page<T> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function, Pageable pageable, Class<T> clazz, TrackHits trackHits) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.from((int) pageable.getOffset());
            builder.size(pageable.getPageSize());
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order ->
                        builder.sort(q -> q.field(f -> f.field(order.getProperty()).order(order.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc)))
                );
            }
            builder.trackTotalHits(trackHits);
            SearchResponse<T> response = ElasticsearchConfig.getInstance().search(function.apply(builder).build(), clazz);
            return plainToClass(response, pageable);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default <T extends EsPayload> Page<T> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function, Pageable pageable, Class<T> clazz) {
        return search(function, pageable, clazz, TrackHits.of(x -> x.enabled(true)));
    }


}
