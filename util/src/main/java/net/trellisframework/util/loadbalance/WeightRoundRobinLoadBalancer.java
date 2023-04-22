package net.trellisframework.util.loadbalance;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightRoundRobinLoadBalancer<T> extends RoundRobinLoadBalancer<T> {

    public WeightRoundRobinLoadBalancer(Map<T, Integer> map) {
        super(map.keySet().stream().map(x -> {
            List<T> tmp = new LinkedList<>();
            for (int i = 0; i < map.get(x); i++) {
                tmp.add(x);
            }
            return tmp;
        }).flatMap(Collection::stream).collect(Collectors.toList()));
    }

    public static <T> WeightRoundRobinLoadBalancer<T> of(Map<T, Integer> map) {
        return new WeightRoundRobinLoadBalancer<>(map);
    }

}