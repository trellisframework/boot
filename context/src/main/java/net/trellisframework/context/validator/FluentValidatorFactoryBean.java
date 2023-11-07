package net.trellisframework.context.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;

public class FluentValidatorFactoryBean extends LocalValidatorFactoryBean {

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof ArrayList objects) {
            for (Object o : objects) {
                super.validate(o, errors);
            }
        } else {
            super.validate(target, errors);
        }
        if (!errors.hasErrors() && target instanceof FluentValidator<?>) {
            ((FluentValidator<?>) target).execute();
        }
    }

}
