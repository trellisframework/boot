package net.trellisframework.context.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.stream.StreamSupport;

public class FluentValidatorFactoryBean extends LocalValidatorFactoryBean {

    @Override
    public void validate(Object target, Errors errors) {
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(target);
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current instanceof Iterable || current instanceof Map<?, ?> || current instanceof Object[]) {
                Object[] objects;
                if (current instanceof Iterable<?> iterable) {
                    objects = StreamSupport.stream(iterable.spliterator(), false).toArray(Object[]::new);
                } else if (current instanceof Map<?, ?> map) {
                    objects = map.values().toArray();
                } else {
                    objects = (Object[]) current;
                }
                for (Object o : objects) {
                    stack.push(o);
                }
            } else {
                super.validate(current, errors);
            }
        }
        if (!errors.hasErrors() && target instanceof FluentValidator<?>) {
            ((FluentValidator<?>) target).execute();
        }
    }


}
