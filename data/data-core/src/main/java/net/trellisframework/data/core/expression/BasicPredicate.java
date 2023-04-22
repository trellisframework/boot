package net.trellisframework.data.core.expression;

import com.querydsl.core.types.dsl.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class BasicPredicate<T> {

  private final SearchCriteria criteria;

  BasicPredicate(SearchCriteria criteria) {
    this.criteria = criteria;
  }

  BooleanExpression getPredicate(Class<T> clazz, String name) {
    PathBuilder<T> entity = new PathBuilder<>(clazz, name);
    if (this.isDate(criteria.getValue())) {
      DatePath<LocalDate> path = entity.getDate(criteria.getKey(), LocalDate.class);
      switch (criteria.getOperation()) {
        case ":":
          return path.eq(LocalDate.parse(criteria.getValue()));
        case "!":
          return path.ne(LocalDate.parse(criteria.getValue()));
        case ">":
          return path.gt(LocalDate.parse(criteria.getValue()));
        case "<":
          return path.lt(LocalDate.parse(criteria.getValue()));
      }
    }

    if (this.isNumber(criteria.getValue())) {
      NumberPath<BigDecimal> path = entity.getNumber(criteria.getKey(), BigDecimal.class);
      switch (criteria.getOperation()) {
        case ":":
          return path.eq(new BigDecimal(criteria.getValue()));
        case "!":
          return path.ne(new BigDecimal(criteria.getValue()));
        case ">":
          return path.gt(new BigDecimal(criteria.getValue()));
        case "<":
          return path.lt(new BigDecimal(criteria.getValue()));
      }
    }

    StringPath path = entity.getString(criteria.getKey());
    if (criteria.getOperation().equalsIgnoreCase(":")) {
      return path.containsIgnoreCase(criteria.getValue());
    } else if (criteria.getOperation().equalsIgnoreCase("!")) {
      return path.ne(criteria.getValue());
    }
    return null;
  }

  private boolean isDate(String value) {
    try {
      LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  private boolean isNumber(String value) {
    try {
      new BigDecimal(value);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

}

