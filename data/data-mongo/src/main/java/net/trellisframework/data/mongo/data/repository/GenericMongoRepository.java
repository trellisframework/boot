package net.trellisframework.data.mongo.data.repository;

import net.trellisframework.data.core.data.repository.GenericRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GenericMongoRepository<TEntity, ID> extends GenericRepository, MongoRepository<TEntity, ID>, QuerydslPredicateExecutor<TEntity> {

}
