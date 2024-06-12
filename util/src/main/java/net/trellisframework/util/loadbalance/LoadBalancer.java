package net.trellisframework.util.loadbalance;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;

@Getter
public abstract class LoadBalancer<T> {
    final List<T> content;

    public LoadBalancer(List<T> content) {
        this.content = Collections.unmodifiableList(content);
    }

    public abstract T next();

    public Boolean hasContent() {
        return ObjectUtils.isNotEmpty(content);
    }
}