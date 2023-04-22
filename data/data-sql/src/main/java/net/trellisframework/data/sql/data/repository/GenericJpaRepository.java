package net.trellisframework.data.sql.data.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.dml.DeleteClause;
import com.querydsl.core.dml.InsertClause;
import com.querydsl.core.dml.UpdateClause;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.core.data.repository.GenericRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GenericJpaRepository<TEntity, ID> extends GenericRepository<TEntity, ID>, JpaRepository<TEntity, ID>, QuerydslPredicateExecutor<TEntity> {
    private JPAQueryFactory getFactory() {
        return ApplicationContextProvider.context.getBean(JPAQueryFactory.class);
    }

    default DeleteClause<?> delete(EntityPath<?> var1) {
        return getFactory().delete(var1);
    }

    default <T> JPQLQuery<T> select(Expression<T> var1) {
        return getFactory().select(var1);
    }

    default JPQLQuery<Tuple> select(Expression<?>... var1) {
        return getFactory().select(var1);
    }

    default <T> JPQLQuery<T> selectDistinct(Expression<T> var1) {
        return getFactory().selectDistinct(var1);
    }

    default JPQLQuery<Tuple> selectDistinct(Expression<?>... var1) {
        return getFactory().selectDistinct(var1);
    }

    default JPQLQuery<Integer> selectOne() {
        return getFactory().selectOne();
    }

    default JPQLQuery<Integer> selectZero() {
        return getFactory().selectZero();
    }

    default <T> JPQLQuery<T> selectFrom(EntityPath<T> var1) {
        return getFactory().selectFrom(var1);
    }

    default JPQLQuery<?> from(EntityPath<?> var1) {
        return getFactory().from(var1);
    }

    default JPQLQuery<?> from(EntityPath<?>... var1) {
        return getFactory().from(var1);
    }

    default UpdateClause<?> update(EntityPath<?> var1) {
        return getFactory().update(var1);
    }

    default InsertClause<?> insert(EntityPath<?> var1) {
        return getFactory().insert(var1);
    }

}
