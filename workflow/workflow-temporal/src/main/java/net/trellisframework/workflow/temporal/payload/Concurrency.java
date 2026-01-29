package net.trellisframework.workflow.temporal.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Concurrency {
    public static final int DEFAULT_LIMIT = 10;
    private String key;
    private int limit;

    public static Concurrency of(String key) {
        return of(key, DEFAULT_LIMIT);
    }

    public static Concurrency of(String key, int limit) {
        Concurrency concurrency = new Concurrency();
        concurrency.key = key;
        concurrency.limit = limit;
        return concurrency;
    }

    public static Concurrency of(String key, Integer limit) {
        Concurrency concurrency = new Concurrency();
        concurrency.key = key;
        concurrency.limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
        return concurrency;
    }

    @JsonIgnore
    public boolean isValid() {
        return Optional.ofNullable(key).map(x -> !x.isEmpty()).orElse(false);
    }

}

