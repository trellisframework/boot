package net.trellisframework.data.elastic.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.data.core.util.DefaultPageRequest;
import net.trellisframework.data.elastic.annotation.Document;
import net.trellisframework.data.elastic.configuration.ElasticsearchConfig;
import net.trellisframework.data.elastic.mapper.EsModelMapper;
import net.trellisframework.data.elastic.model.CoreDocument;
import net.trellisframework.data.elastic.payload.ElasticRequest;
import net.trellisframework.http.exception.BadGatewayException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.util.object.ObjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoRepositoryBean
public interface GenericElasticRepository<TEntity extends CoreDocument> extends GenericRepository, EsModelMapper {

    private Class<TEntity> getEntityClass() {
        return (Class<TEntity>) (((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
    }

    default String index_name() {
        Class<TEntity> clazz = getEntityClass();
        if (clazz.isAnnotationPresent(Document.class))
            return clazz.getAnnotation(Document.class).value();
        return StringUtils.lowerCase(StringUtils.replace(StringUtils.replaceIgnoreCase(clazz.getSimpleName(), "entity", ""), "document", ""));
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz) {
        return search(fn, clazz, TrackHits.of(x -> x.enabled(true)));
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz, TrackHits trackHits) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.trackTotalHits(trackHits);
            return ElasticsearchConfig.getInstance().search(fn.apply(builder).build(), clazz);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default Long count(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn) {
        try {
            ElasticRequest request = fn.apply(ElasticRequest.builder());
            CountRequest.Builder builder = new CountRequest.Builder();
            builder.index(ObjectUtil.defaultIfEmpty(request.getIndex(), List.of(index_name())));
            Optional.ofNullable(request.getFilters()).ifPresent(builder::query);
            return Optional.ofNullable(ElasticsearchConfig.getInstance().count(s -> builder)).map(CountResponse::count).orElse(0L);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default List<TEntity> findAll(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn) {
        return findAll(fn, getEntityClass());
    }

    default <TDocument extends CoreDocument> List<TDocument> findAll(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, Class<TDocument> clazz) {
        return findAll(fn, Pageable.unpaged(), clazz).getContent();
    }

    default Page<TEntity> findAll(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, Pageable pageable) {
        return findAll(fn, pageable, getEntityClass());
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, Pageable pageable, Class<TDocument> clazz) {
        try {
            ElasticRequest request = fn.apply(ElasticRequest.builder());
            SearchRequest.Builder builder = new SearchRequest.Builder();
            Optional.ofNullable(request.getCollapse()).ifPresent(x -> builder.collapse(c -> c.field(x)));
            if (Optional.ofNullable(pageable).map(Pageable::isPaged).orElse(false)) {
                builder.from(Math.toIntExact(pageable.getOffset()));
                builder.size(pageable.getPageSize());
                if (pageable.getSort().isSorted()) {
                    pageable.getSort().forEach(order ->
                            builder.sort(q -> q.field(f -> f.field(order.getProperty()).order(order.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc)))
                    );
                }
            }
            builder.index(ObjectUtil.defaultIfEmpty(request.getIndex(), List.of(index_name())));
            Optional.ofNullable(request.getFilters()).ifPresent(builder::query);
            Optional.ofNullable(ObjectUtil.nullIfEmpty(request.getSources())).ifPresent(x -> builder.source(s -> s.filter(f -> f.includes(x))));
            builder.trackTotalHits(request.getTrackHits());
            SearchResponse<TDocument> response = ElasticsearchConfig.getInstance().search(s -> builder, clazz);
            return plainToClass(response, pageable);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default List<String> findIds(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn) {
        return findIds(fn, Pageable.unpaged());
    }

    default List<String> findIds(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, Pageable pageable) {
        try {
            ElasticRequest request = fn.apply(ElasticRequest.builder());
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(ObjectUtil.defaultIfEmpty(request.getIndex(), List.of(index_name())));
            builder.source(b -> b.filter(f -> f.includes("id")));
            Optional.ofNullable(request.getFilters()).ifPresent(builder::query);
            if (Optional.ofNullable(pageable).map(Pageable::isPaged).orElse(false)) {
                builder.from((int) pageable.getOffset());
                builder.size(pageable.getPageSize());
                if (pageable.getSort().isSorted()) {
                    pageable.getSort().forEach(order ->
                            builder.sort(q -> q.field(f -> f.field(order.getProperty()).order(order.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc)))
                    );
                }
            }
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).hits().hits().stream().map(Hit::id).toList();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    default <TDocument extends CoreDocument> TDocument save(TDocument entity) {
        try {
            entity.setId(StringUtils.defaultIfBlank(entity.getId(), UUID.randomUUID().toString()));
            ElasticsearchConfig.getInstance().index(b -> b.index(index_name()).document(entity).id(entity.getId()));
            return entity;
        } catch (Exception e) {
            Logger.error("GenericElasticRepository::save", index_name() + " " + JsonUtil.toString(entity));
        }
        return null;
    }

    default Optional<TEntity> findById(String id) {
        return findById(id, getEntityClass());
    }

    default <TDocument extends CoreDocument> Optional<TDocument> findById(String id, Class<TDocument> clazz) {
        return findAll(s -> s.filters(q -> q.ids(i -> i.values(id))).build(), DefaultPageRequest.of(0, 1), clazz).stream().findFirst();
    }

    default Map<String, Aggregate> aggregations(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, Map<String, Aggregation> aggregation) {
        try {
            ElasticRequest request = fn.apply(ElasticRequest.builder());
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(ObjectUtil.defaultIfEmpty(request.getIndex(), List.of(index_name())));
            builder.size(0);
            builder.aggregations(aggregation);
            Optional.ofNullable(request.getFilters()).ifPresent(builder::query);
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).aggregations();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    default Map<String, Aggregate> aggregations(Function<ElasticRequest.ElasticRequestBuilder, ElasticRequest> fn, String key, Function<Aggregation.Builder, ObjectBuilder<Aggregation>> aggregation) {
        try {
            ElasticRequest request = fn.apply(ElasticRequest.builder());
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(ObjectUtil.defaultIfEmpty(request.getIndex(), List.of(index_name())));
            builder.size(0);
            builder.aggregations(key, aggregation);
            Optional.ofNullable(request.getFilters()).ifPresent(builder::query);
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).aggregations();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

}
