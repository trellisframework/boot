package net.trellisframework.workflow.temporal.payload;

import lombok.Getter;

import java.util.Optional;


@Getter
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

    public boolean isValid() {
        return Optional.ofNullable(key).map(x -> !x.isEmpty()).orElse(false);
    }

}

