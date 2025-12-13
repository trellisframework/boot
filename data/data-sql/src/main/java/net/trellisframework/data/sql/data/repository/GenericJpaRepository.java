package net.trellisframework.data.sql.data.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.dml.DeleteClause;
import com.querydsl.core.dml.InsertClause;
import com.querydsl.core.dml.UpdateClause;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilderFactory;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.core.data.repository.GenericRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

@NoRepositoryBean
public interface GenericJpaRepository<TEntity, ID> extends GenericRepository, JpaRepository<TEntity, ID>, QuerydslPredicateExecutor<TEntity> {

    default JPAQueryFactory getFactory() {
        return ApplicationContextProvider.context.getBean(JPAQueryFactory.class);
    }

    default EntityManager getEntityManager() {
        return ApplicationContextProvider.context.getBean(EntityManager.class);
    }

    default DeleteClause<?> delete(EntityPath<?> var1) {
        return getFactory().delete(var1);
    }

    default <T> JPAQuery<T> select(Expression<T> var1) {
        return getFactory().select(var1);
    }

    default <T> JPAQuery<T> select(Expression<T> var1, EntityGraphType type, String graphName) {
        return getFactory().select(var1).setHint(type.getKey(), getEntityManager().getEntityGraph(graphName));
    }

    default JPAQuery<Tuple> select(Expression<?>... var1) {
        return getFactory().select(var1);
    }

    default JPAQuery<Tuple> select(EntityGraphType type, String graphName, Expression<?>... var1) {
        return getFactory().select(var1).setHint(type.getKey(), getEntityManager().getEntityGraph(graphName));
    }

    default <T> JPAQuery<T> selectDistinct(Expression<T> var1) {
        return getFactory().selectDistinct(var1);
    }

    default JPAQuery<Tuple> selectDistinct(Expression<?>... var1) {
        return getFactory().selectDistinct(var1);
    }

    default JPAQuery<Integer> selectOne() {
        return getFactory().selectOne();
    }

    default JPAQuery<Integer> selectZero() {
        return getFactory().selectZero();
    }

    default <T> JPAQuery<T> selectFrom(EntityPath<T> var1) {
        return getFactory().selectFrom(var1);
    }

    default JPAQuery<?> from(EntityPath<?> var1) {
        return getFactory().from(var1);
    }

    default JPAQuery<?> from(EntityPath<?>... var1) {
        return getFactory().from(var1);
    }

    default UpdateClause<?> update(EntityPath<?> var1) {
        return getFactory().update(var1);
    }

    default InsertClause<?> insert(EntityPath<?> var1) {
        return getFactory().insert(var1);
    }

    default <T> Page<T> findAll(JPQLQuery<T> query, Pageable pageable) {
        Querydsl querydsl = new Querydsl(getEntityManager(), (new PathBuilderFactory()).create(query.getType()));
        return PageableExecutionUtils.getPage(querydsl.applyPagination(pageable, query).fetch(), pageable, query::fetchCount);
    }

    default TEntity upsert(EntityPath<TEntity> entityPath, BooleanExpression condition, Function<TEntity, TEntity> update, Supplier<TEntity> insert) {
        TEntity existing = getFactory().selectFrom(entityPath).where(condition).fetchOne();
        if (existing != null) {
            TEntity updated = update.apply(existing);
            return save(updated);
        } else {
            TEntity entity = insert.get();
            return save(entity);
        }
    }
}
