package net.trellisframework.data.core.expression;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Visitor;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;
import java.util.function.Function;
public final class BooleanBuilder implements Predicate, Cloneable {

    private Predicate predicate;

    public BooleanBuilder() {
    }

    public BooleanBuilder(Predicate initial) {
        predicate = (Predicate) ExpressionUtils.extract(initial);
    }

    public static BooleanBuilder of() {
        return new BooleanBuilder();
    }

    public static BooleanBuilder of(Predicate initial) {
        return new BooleanBuilder(initial);
    }

    public <V> BooleanBuilder and(V value, LazyBooleanExpression expression) {
        return applyIfNotNull(value, this::and, expression);
    }

    public <V> BooleanBuilder or(V value, LazyBooleanExpression expression) {
        return applyIfNotNull(value, this::or, expression);
    }

    private <V> BooleanBuilder applyIfNotNull(V value, Function<Predicate, BooleanBuilder> function, LazyBooleanExpression expression) {
        return ObjectUtils.isNotEmpty(value) ? new BooleanBuilder(function.apply(expression.get())) : this;
    }

    public BooleanBuilder and(Predicate right) {
        if (right != null) {
            if (predicate == null) {
                predicate = right;
            } else {
                predicate = ExpressionUtils.and(predicate, right);
            }
        }
        return this;
    }

    public BooleanBuilder andAnyOf(Predicate... args) {
        if (args.length > 0) {
            and(ExpressionUtils.anyOf(args));
        }
        return this;
    }

    BooleanBuilder andNot(Predicate right) {
        return and(right.not());
    }

    public Predicate getValue() {
        return predicate;
    }

    public boolean hasValue() {
        return predicate != null;
    }

    public BooleanBuilder or(Predicate right) {
        if (right != null) {
            if (predicate == null) {
                predicate = right;
            } else {
                predicate = ExpressionUtils.or(predicate, right);
            }
        }
        return this;
    }

    public BooleanBuilder orAllOf(Predicate... args) {
        if (args.length > 0) {
            or(ExpressionUtils.allOf(args));
        }
        return this;
    }

    public BooleanBuilder orNot(Predicate right) {
        return or(right.not());
    }

    @Override
    public <R, C> R accept(Visitor<R, C> v, C context) {
        if (predicate != null) {
            return predicate.accept(v, context);
        } else {
            return null;
        }
    }

    @Override
    public BooleanBuilder clone() throws CloneNotSupportedException {
        return (BooleanBuilder) super.clone();
    }

    @Override
    public BooleanBuilder not() {
        if (predicate != null) {
            predicate = predicate.not();
        }
        return this;
    }

    @Override
    public Class<? extends Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof BooleanBuilder) {
            return Objects.equals(((BooleanBuilder) o).getValue(), predicate);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return predicate != null ? predicate.hashCode() : 0;
    }

    @Override
    public String toString() {
        return predicate != null ? predicate.toString() : super.toString();
    }

}