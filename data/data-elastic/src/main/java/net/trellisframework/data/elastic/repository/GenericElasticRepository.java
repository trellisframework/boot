package net.trellisframework.data.elastic.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.core.util.DefaultPageRequest;
import net.trellisframework.data.elastic.annotation.Document;
import net.trellisframework.data.elastic.configuration.ElasticsearchConfig;
import net.trellisframework.data.elastic.model.CoreDocument;
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
public interface GenericElasticRepository<TEntity extends CoreDocument> extends PagingAndSortingElasticRepository<TEntity> {

    private Class<TEntity> getEntityClass() {
        return (Class<TEntity>) (((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
    }

    default String index_name() {
        Class<TEntity> clazz = getEntityClass();
        if (clazz.isAnnotationPresent(Document.class))
            return clazz.getAnnotation(Document.class).value();
        return StringUtils.lowerCase(StringUtils.replace(StringUtils.replaceIgnoreCase(clazz.getSimpleName(), "entity", ""), "document", ""));
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function, Class<TDocument> clazz) {
        return search(function, clazz, TrackHits.of(x -> x.enabled(true)));
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function, Class<TDocument> clazz, TrackHits trackHits) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.trackTotalHits(trackHits);
            return ElasticsearchConfig.getInstance().search(function.apply(builder).build(), clazz);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default List<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query) {
        return findAll(query, Pageable.unpaged()).getContent();
    }

    default Page<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, Pageable pageable) {
        return findAll(query, null, pageable);
    }

    default List<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes) {
        return findAll(query, includes, Pageable.unpaged()).getContent();
    }

    default Page<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, Pageable pageable) {
        return findAll(query, includes, pageable, TrackHits.of(x -> x.enabled(true)));
    }

    default List<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, TrackHits trackHits) {
        return findAll(query, includes, Pageable.unpaged(), trackHits).getContent();
    }

    default Page<TEntity> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, Pageable pageable, TrackHits trackHits) {
        return findAll(query, includes, pageable, trackHits, getEntityClass());
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, Class<TDocument> clazz) {
        return findAll(query, Pageable.unpaged(), clazz);
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, Pageable pageable, Class<TDocument> clazz) {
        return findAll(query, null, pageable, clazz);
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, Class<TDocument> clazz) {
        return findAll(query, includes, Pageable.unpaged(), clazz);
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, Pageable pageable, Class<TDocument> clazz) {
        return findAll(query, includes, pageable, TrackHits.of(x -> x.enabled(true)), clazz);
    }



    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, TrackHits trackHits, Class<TDocument> clazz) {
        return findAll(query, includes, Pageable.unpaged(), trackHits, clazz);
    }

    default <TDocument extends CoreDocument> Page<TDocument> findAll(Function<Query.Builder, ObjectBuilder<Query>> query, List<String> includes, Pageable pageable, TrackHits trackHits, Class<TDocument> clazz) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            if (Optional.ofNullable(pageable).map(Pageable::isPaged).orElse(false)) {
                builder.from(Math.toIntExact(pageable.getOffset()));
                builder.size(pageable.getPageSize());
                if (pageable.getSort().isSorted()) {
                    pageable.getSort().forEach(order ->
                            builder.sort(q -> q.field(f -> f.field(order.getProperty()).order(order.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc)))
                    );
                }
            }
            builder.index(index_name());
            builder.query(query);
            Optional.ofNullable(ObjectUtil.nullIfEmpty(includes)).ifPresent(x -> builder.source(s -> s.filter(f -> f.includes(x))));
            builder.trackTotalHits(trackHits);
            SearchResponse<TDocument> response = ElasticsearchConfig.getInstance().search(s -> builder, clazz);
            return plainToClass(response, pageable);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default List<String> findIds(Function<Query.Builder, ObjectBuilder<Query>> query) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(index_name());
            builder.source(b -> b.filter(f -> f.includes("id")));
            builder.query(query);
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).hits().hits().stream().map(Hit::id).toList();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    default List<String> findIds(Function<Query.Builder, ObjectBuilder<Query>> query, Pageable pageable) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(index_name());
            builder.source(b -> b.filter(f -> f.includes("id")));
            builder.query(query);
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
        return findAll(q -> q.ids(i -> i.values(id)), DefaultPageRequest.of(0, 1), clazz).stream().findFirst();
    }

    default Map<String, Aggregate> aggregations(Function<Query.Builder, ObjectBuilder<Query>> query, Map<String, Aggregation> aggregation) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(index_name());
            builder.size(0);
            builder.aggregations(aggregation);
            builder.query(query);
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).aggregations();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

    default Map<String, Aggregate> aggregations(Function<Query.Builder, ObjectBuilder<Query>> query, String key, Function<Aggregation.Builder, ObjectBuilder<Aggregation>> aggregation) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder();
            builder.index(index_name());
            builder.size(0);
            builder.aggregations(key, aggregation);
            builder.query(query);
            return ElasticsearchConfig.getInstance().search(s -> builder, Object.class).aggregations();
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
    }

}
