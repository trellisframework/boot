package net.trellisframework.data.elastic.repository;

import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.core.update.UpdateWriteResponseBase;
import co.elastic.clients.util.ObjectBuilder;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.data.core.util.DefaultPageRequest;
import net.trellisframework.data.elastic.annotation.Document;
import net.trellisframework.data.elastic.configuration.ElasticsearchConfig;
import net.trellisframework.data.elastic.mapper.EsModelMapper;
import net.trellisframework.http.exception.ServiceUnavailableException;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.util.reflection.ReflectionUtil;
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
public interface GenericElasticRepository<TEntity> extends GenericRepository, EsModelMapper {

    default Class<TEntity> getEntityClass() {
        return (Class<TEntity>) (((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
    }

    default String index_name() {
        Class<TEntity> clazz = getEntityClass();
        if (clazz.isAnnotationPresent(Document.class))
            return clazz.getAnnotation(Document.class).value();
        return StringUtils.lowerCase(StringUtils.replace(StringUtils.replaceIgnoreCase(clazz.getSimpleName(), "entity", ""), "document", ""));
    }

    default Long count(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
        return count(fn, false);
    }

    default Long count(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Boolean hasJoin) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.size(0).trackTotalHits(TrackHits.of(x -> x.enabled(true)));
        return Optional.ofNullable(search(s -> fn.apply(builder), Object.class, hasJoin)).map(ResponseBody::hits).map(HitsMetadata::total).map(TotalHits::value).orElse(0L);
    }

    default List<TEntity> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
        return findAll(fn, false);
    }

    default List<TEntity> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Boolean hasJoin) {
        return findAll(fn, getEntityClass(), hasJoin);
    }

    default Page<TEntity> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable) {
        return findAll(fn, pageable, false);
    }

    default Page<TEntity> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Boolean hasJoin) {
        return findAll(fn, pageable, getEntityClass(), hasJoin);
    }

    default <TDocument> List<TDocument> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz) {
        return findAll(fn, clazz, false);
    }

    default <TDocument> List<TDocument> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz, Boolean hasJoin) {
        return findAll(fn, Pageable.unpaged(), clazz, hasJoin).getContent();
    }

    default <TDocument> Page<TDocument> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Class<TDocument> clazz) {
        return findAll(fn, pageable, clazz, false);
    }

    default <TDocument> Page<TDocument> findAll(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Class<TDocument> clazz, Boolean hasJoin) {
        return plainToClass(search(fn, pageable, clazz, hasJoin), pageable);
    }

    default List<String> findIds(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
        return findIds(fn, false);
    }

    default List<String> findIds(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Boolean hasJoin) {
        return findIds(fn, Pageable.unpaged(), hasJoin);
    }

    default List<String> findIds(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable) {
        return findIds(fn, pageable, false);
    }

    default List<String> findIds(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Boolean hasJoin) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.source(s -> s.filter(f -> f.includes("_id")));
        return search(s -> fn.apply(builder), pageable, Object.class, hasJoin).hits().hits().stream().map(Hit::id).toList();
    }

    default Optional<TEntity> findById(String id) {
        return findById(id, getEntityClass());
    }

    default <TDocument> Optional<TDocument> findById(String id, Class<TDocument> clazz) {
        return findAll(s -> s.query(q -> q.ids(i -> i.values(id))), DefaultPageRequest.of(0, 1), clazz).stream().findFirst();
    }

    default Map<String, Aggregate> aggregations(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
        return aggregations(fn, false);
    }

    default Map<String, Aggregate> aggregations(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Boolean hasJoin) {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.size(0);
        return search(s -> fn.apply(builder), Object.class, hasJoin).aggregations();
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz) {
        return search(fn, clazz, false);
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Class<TDocument> clazz, Boolean hasJoin) {
        return search(fn, Pageable.unpaged(), clazz, hasJoin);
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Class<TDocument> clazz) {
        return search(fn, pageable, clazz, false);
    }

    default <TDocument> SearchResponse<TDocument> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn, Pageable pageable, Class<TDocument> clazz, Boolean hasJoin) {
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
            return hasJoin ? ElasticsearchConfig.getInstance().sirenSearch(s -> fn.apply(builder), clazz) : ElasticsearchConfig.getInstance().search(s -> fn.apply(builder), clazz);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default <TDocument> TDocument update(Function<UpdateRequest.Builder<TDocument, TDocument>, ObjectBuilder<UpdateRequest<TDocument, TDocument>>> fn, Class<TDocument> clazz) {
        return Optional.ofNullable(updated(fn, clazz)).map(UpdateWriteResponseBase::get).map(InlineGet::source).orElse(null);
    }

    default <TDocument> UpdateResponse<TDocument> updated(Function<UpdateRequest.Builder<TDocument, TDocument>, ObjectBuilder<UpdateRequest<TDocument, TDocument>>> fn, Class<TDocument> clazz) {
        try {
            return ElasticsearchConfig.getInstance().update(fn, clazz);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default DeleteResponse delete(Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn) {
        try {
            return ElasticsearchConfig.getInstance().delete(fn);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default UpdateByQueryResponse updateByQuery(Function<UpdateByQueryRequest.Builder, ObjectBuilder<UpdateByQueryRequest>> fn) {
        try {
            return ElasticsearchConfig.getInstance().updateByQuery(fn);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default DeleteByQueryResponse deleteByQuery(Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>> fn) {
        try {
            return ElasticsearchConfig.getInstance().deleteByQuery(fn);
        } catch (IOException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    default <TDocument> TDocument save(TDocument entity) {
        try {
            String id = ReflectionUtil.getPropertyValue(entity, "id", UUID.randomUUID().toString());
            ElasticsearchConfig.getInstance().index(b -> b.index(index_name()).document(entity).id(id));
            return entity;
        } catch (Exception e) {
            Logger.error("GenericElasticRepository::save", index_name() + " " + JsonUtil.toString(entity));
        }
        return null;
    }


}
