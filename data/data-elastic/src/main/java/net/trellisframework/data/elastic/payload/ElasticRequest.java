package net.trellisframework.data.elastic.payload;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.ObjectBuilder;
import lombok.Builder;
import lombok.Getter;
import net.trellisframework.core.payload.Payload;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Builder
public class ElasticRequest implements Payload {
    private List<String> index;

    private Function<Query.Builder, ObjectBuilder<Query>> filters;

    private List<String> sources;

    private String collapse;

    @Builder.Default
    private TrackHits trackHits = TrackHits.of(x -> x.enabled(true));

    public static class ElasticRequestBuilder {
        public ElasticRequestBuilder index(String... index) {
            return index(Optional.ofNullable(index).map(List::of).orElse(null));
        }

        public ElasticRequestBuilder index(List<String> index) {
            if (ObjectUtils.isNotEmpty(index)) {
                if (this.index == null)
                    this.index = new ArrayList<>();
                this.index.addAll(index);
            }
            return this;
        }

        public ElasticRequestBuilder sources(String... source) {
            return sources(Optional.ofNullable(source).map(List::of).orElse(null));
        }

        public ElasticRequestBuilder sources(List<String> sources) {
            if (ObjectUtils.isNotEmpty(sources)) {
                if (this.sources == null)
                    this.sources = new ArrayList<>();
                this.sources.addAll(sources);
            }
            return this;
        }
    }

}
