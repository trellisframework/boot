package net.trellisframework.validator;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintViolationCreationContext;

import java.lang.annotation.*;
import java.util.Collection;
import java.util.Map;

@Documented
@Constraint(validatedBy = {Required.RequiredValidator.class, Required.RequiredCollectionValidator.class, Required.RequiredArrayValidator.class, Required.RequiredMapValidator.class})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
    String name() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    abstract class AbstractRequiredValidator<T> implements ConstraintValidator<Required, T> {
        private Required annotation;

        @Override
        public void initialize(Required annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isValid(T field, ConstraintValidatorContext cxt) {
            if (!isValid(field)) {
                String message = annotation.message();
                if (StringUtils.isEmpty(message)) {
                    ConstraintViolationCreationContext constraintValidatorContext = ((ConstraintValidatorContextImpl) cxt).getConstraintViolationCreationContexts().parallelStream().findFirst().orElse(null);
                    String field_name = (StringUtils.isNotBlank(annotation.name()) ? annotation.name() : constraintValidatorContext == null ? StringUtils.EMPTY : constraintValidatorContext.getPath().getLeafNode().getName()).replaceAll("([A-Z])", "_$1");
                    message = ((StringUtils.isEmpty(field_name) ? "FIELD" : field_name) + "_IS_REQUIRED").toUpperCase();
                }
                cxt.disableDefaultConstraintViolation();
                cxt.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                return false;
            }
            return true;
        }

        protected abstract boolean isValid(T field);
    }

    class RequiredValidator extends AbstractRequiredValidator<Object> {

        @Override
        protected boolean isValid(Object field) {
            return !(ObjectUtils.isEmpty(field) || StringUtils.isBlank(field.toString()));
        }
    }

    class RequiredCollectionValidator extends AbstractRequiredValidator<Collection> {
        @Override
        protected boolean isValid(Collection field) {
            return !(ObjectUtils.isEmpty(field));
        }
    }

    class RequiredArrayValidator extends AbstractRequiredValidator<Object[]> {
        @Override
        protected boolean isValid(Object[] field) {
            return !(ObjectUtils.isEmpty(field));
        }
    }

    class RequiredMapValidator extends AbstractRequiredValidator<Map<?,?>> {
        @Override
        protected boolean isValid(Map<?,?> field) {
            return !(ObjectUtils.isEmpty(field));
        }
    }
}
