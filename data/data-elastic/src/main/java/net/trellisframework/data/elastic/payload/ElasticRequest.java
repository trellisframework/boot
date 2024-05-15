package net.trellisframework.data.elastic.payload;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import lombok.Builder;
import lombok.Getter;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.data.elastic.model.CoreDocument;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Builder
public class ElasticRequest implements Payload {
    private List<String> indices;

    private Function<Query.Builder, ObjectBuilder<Query>> filters;

    private List<String> sources;

    private String collapse;

    @Builder.Default
    private TrackHits trackHits = TrackHits.of(x -> x.enabled(true));

    public static class ElasticRequestBuilder {
        public ElasticRequestBuilder indices(String... indices) {
            return indices(Optional.ofNullable(indices).map(List::of).orElse(null));
        }

        public ElasticRequestBuilder indices(List<String> indices) {
            if (ObjectUtils.isNotEmpty(indices))
                if (this.indices == null)
                    this.indices = new ArrayList<>();
                this.indices.addAll(indices);
            return this;
        }
    }

}
