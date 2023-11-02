package net.trellisframework.context.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class FluentValidatorFactoryBean extends LocalValidatorFactoryBean {

    @Override
    public void validate(Object target, Errors errors) {
        super.validate(target, errors);
        if (!errors.hasErrors() && target instanceof FluentValidator<?>) {
            ((FluentValidator<?>) target).execute();
        }
    }

}
