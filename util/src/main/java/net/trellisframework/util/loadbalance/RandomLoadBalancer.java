package net.trellisframework.util.loadbalance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer<T> extends LoadBalancer<T> {

    public RandomLoadBalancer(List<T> values) {
        super(values);
    }

    public static <T> RandomLoadBalancer<T> of(List<T> values) {
        return new RandomLoadBalancer<>(values);
    }

    public static <T> RandomLoadBalancer<T> of(T[] values) {
        return new RandomLoadBalancer<>(List.of(values));
    }

    @Override
    public T getNextValue() {
        return values.get(new Random().nextInt(values.size()));
    }
}