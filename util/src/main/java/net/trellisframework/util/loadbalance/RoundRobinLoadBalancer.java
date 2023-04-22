package net.trellisframework.util.loadbalance;

import java.util.List;

public class RoundRobinLoadBalancer<T> extends LoadBalancer<T> {

    int index = 0;

    public RoundRobinLoadBalancer(List<T> values) {
        super(values);
    }

    public static <T> RoundRobinLoadBalancer<T> of(List<T> values) {
        return new RoundRobinLoadBalancer<>(values);
    }

    public static <T> RoundRobinLoadBalancer<T> of(T[] values) {
        return new RoundRobinLoadBalancer<>(List.of(values));
    }

    @Override
    public T getNextValue() {
        synchronized ("RoundRobinLoadBalancer") {
            T value = values.get(index);
            index++;
            if (index == values.size())
                index = 0;
            return value;
        }
    }
}