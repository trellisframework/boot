package net.trellisframework.data.core.expression;

import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

import java.util.Optional;
import java.util.Set;
public class StringExpressions {

    public static BooleanExpression anyIgnoreCase(Expression<String> str, String... values) {
        return anyIgnoreCase(str, Set.of(values));
    }

    public static BooleanExpression anyIgnoreCase(Expression<String> str, Set<String> values) {
        BooleanExpression operation = null;
        for (String value : values) {
            operation = Optional.ofNullable(operation)
                    .map(x -> x.or(Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, str, ConstantImpl.create(value))))
                    .orElse(Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, str, ConstantImpl.create(value)));
        }
        return operation;
    }

    public static BooleanExpression any(Expression<String> str, String... values) {
        return any(str, Set.of(values));
    }

    public static BooleanExpression any(Expression<String> str, Set<String> values) {
        BooleanExpression operation = null;
        for (String value : values) {
            operation = Optional.ofNullable(operation)
                    .map(x -> x.or(Expressions.booleanOperation(Ops.STRING_CONTAINS, str, ConstantImpl.create(value))))
                    .orElse(Expressions.booleanOperation(Ops.STRING_CONTAINS, str, ConstantImpl.create(value)));
        }
        return operation;
    }


    public static BooleanExpression allIgnoreCase(Expression<String> str, String... values) {
        return allIgnoreCase(str, Set.of(values));
    }

    public static BooleanExpression allIgnoreCase(Expression<String> str, Set<String> values) {
        BooleanExpression operation = null;
        for (String value : values) {
            operation = Optional.ofNullable(operation)
                    .map(x -> x.and(Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, str, ConstantImpl.create(value))))
                    .orElse(Expressions.booleanOperation(Ops.STRING_CONTAINS_IC, str, ConstantImpl.create(value)));
        }
        return operation;
    }

    public static BooleanExpression all(Expression<String> str, String... values) {
        return all(str, Set.of(values));
    }

    public static BooleanExpression all(Expression<String> str, Set<String> values) {
        BooleanExpression operation = null;
        for (String value : values) {
            operation = Optional.ofNullable(operation)
                    .map(x -> x.and(Expressions.booleanOperation(Ops.STRING_CONTAINS, str, ConstantImpl.create(value))))
                    .orElse(Expressions.booleanOperation(Ops.STRING_CONTAINS, str, ConstantImpl.create(value)));
        }
        return operation;
    }
}
