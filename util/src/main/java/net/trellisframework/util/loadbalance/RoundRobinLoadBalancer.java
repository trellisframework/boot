package net.trellisframework.util.loadbalance;

import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer<T> extends LoadBalancer<T> {

    private final AtomicInteger index;

    public RoundRobinLoadBalancer(List<T> content) {
        super(content);
        this.index = new AtomicInteger(0);
    }

    public static <T> RoundRobinLoadBalancer<T> of(List<T> values) {
        return new RoundRobinLoadBalancer<>(values);
    }

    public static <T> RoundRobinLoadBalancer<T> of(T[] values) {
        return new RoundRobinLoadBalancer<>(List.of(values));
    }

    @Override
    public T next() {
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }
        int currentIndex = index.getAndUpdate(i -> (i + 1) % content.size());
        return content.get(currentIndex);
    }
}