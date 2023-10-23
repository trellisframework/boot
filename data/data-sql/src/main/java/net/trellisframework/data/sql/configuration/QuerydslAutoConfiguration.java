package net.trellisframework.data.sql.configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Configuration
public class QuerydslAutoConfiguration {

    @PersistenceContext
    private final EntityManager entityManager;

    public QuerydslAutoConfiguration(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @ConditionalOnMissingBean
    @Bean
    public JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
