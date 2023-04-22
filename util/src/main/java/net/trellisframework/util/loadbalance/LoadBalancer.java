package net.trellisframework.util.loadbalance;

import java.util.Collections;
import java.util.List;

public abstract class LoadBalancer<T> {
    final List<T> values;

    public LoadBalancer(List<T> values) {
        this.values = Collections.unmodifiableList(values);
    }

    public abstract T getNextValue();
}