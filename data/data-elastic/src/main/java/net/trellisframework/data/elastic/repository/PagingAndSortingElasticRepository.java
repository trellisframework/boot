package net.trellisframework.data.elastic.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import net.trellisframework.core.log.Logger;
import net.trellisframework.data.core.data.repository.GenericRepository;
import net.trellisframework.data.core.util.DefaultPageRequest;
import net.trellisframework.data.elastic.annotation.Document;
import net.trellisframework.data.elastic.configuration.ElasticsearchConfig;
import net.trellisframework.data.elastic.mapper.EsModelMapper;
import net.trellisframework.data.elastic.model.CoreDocument;
import net.trellisframework.http.exception.ServiceUnavailableException;
import net.trellisframework.util.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoRepositoryBean
public interface PagingAndSortingElasticRepository<TEntity extends CoreDocument> extends GenericRepository, EsModelMapper {


}
