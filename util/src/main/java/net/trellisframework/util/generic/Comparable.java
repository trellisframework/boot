package net.trellisframework.util.generic;

import java.util.Collection;
import java.util.List;

public interface Comparable<T> {

    default boolean isIn(Collection<T> values) {
        for (T current : values) {
            if (this.equals(current)) {
                return true;
            }
        }
        return false;
    }

    default boolean isIn(T... values) {
        return isIn(List.of(values));
    }

    default boolean notEqual(T value) {
        return !equals(value);
    }

    default boolean notIn(Collection<T> values) {
        for (T current : values) {
            if (this.equals(current)) {
                return false;
            }
        }
        return true;
    }

    default boolean notIn(T... values) {
        return notIn(List.of(values));
    }

    default boolean equalAll(T... values) {
        return equalAll(List.of(values));
    }

    default boolean equalAll(Collection<T> values) {
        for (T current : values) {
            if (!this.equals(current)) {
                return false;
            }
        }
        return true;
    }
}
