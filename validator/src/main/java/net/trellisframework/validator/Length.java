package net.trellisframework.validator;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintViolationCreationContext;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.io.Serializable;
import java.lang.annotation.*;
import java.text.MessageFormat;

@Documented
@Constraint(validatedBy = Length.LengthValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Length {
    String name() default "";

    String message() default "";

    long min() default 0;

    long max() default Long.MAX_VALUE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class LengthValidator implements ConstraintValidator<Length, Serializable> {
        private Length annotation;

        @Override
        public void initialize(Length annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isValid(Serializable field, ConstraintValidatorContext cxt) {
            if (field == null || StringUtils.isEmpty(field.toString()))
                return true;
            Integer value = field.toString().length();
            if (annotation.min() > value || annotation.max() < value) {
                String message = annotation.message();
                if (StringUtils.isEmpty(message)) {
                    ConstraintViolationCreationContext constraintValidatorContext = ((ConstraintValidatorContextImpl) cxt).getConstraintViolationCreationContexts().parallelStream().findFirst().orElse(null);
                    String field_name = (StringUtils.isNotBlank(annotation.name()) ? annotation.name() : constraintValidatorContext == null ? StringUtils.EMPTY : constraintValidatorContext.getPath().getLeafNode().getName()).replaceAll("([A-Z])", "_$1");
                    message = ((StringUtils.isEmpty(field_name) ? "FIELD" : field_name) + "_LENGTH_MUST_BE_" + (annotation.min() > value ? "GREATER" : "LESS") + "_OR_EQUAL_THAN {0}").toUpperCase();
                    message = MessageFormat.format(message, annotation.min() > value ? annotation.min() : annotation.max());
                }
                cxt.disableDefaultConstraintViolation();
                cxt.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
