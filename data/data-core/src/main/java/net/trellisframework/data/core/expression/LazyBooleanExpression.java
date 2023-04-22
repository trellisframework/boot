package net.trellisframework.data.core.expression;

import com.querydsl.core.types.dsl.BooleanExpression;

@FunctionalInterface
public interface LazyBooleanExpression {
    BooleanExpression get();
}