package net.trellisframework.validator;


import net.trellisframework.core.constant.Country;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintViolationCreationContext;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import java.text.MessageFormat;

@Documented
@Constraint(validatedBy = Phone.MobileValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Phone {
    String name() default "";

    String message() default "";

    Country[] countries() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MobileValidator implements ConstraintValidator<Phone, String> {
        private Phone annotation;

        @Override
        public void initialize(Phone annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext cxt) {
            if (!StringUtils.isEmpty(s) && !Validator.isPhone(s, annotation.countries())) {
                String message = annotation.message();
                if (StringUtils.isEmpty(message)) {
                    ConstraintViolationCreationContext constraintValidatorContext = ((ConstraintValidatorContextImpl) cxt).getConstraintViolationCreationContexts().parallelStream().findFirst().orElse(null);
                    String field_name = (StringUtils.isNotBlank(annotation.name()) ? annotation.name() : constraintValidatorContext == null ? StringUtils.EMPTY : constraintValidatorContext.getPath().getLeafNode().getName()).replaceAll("([A-Z])", "_$1");
                    message = (MessageFormat.format("{0}_IS_INVALID", StringUtils.isEmpty(field_name) ? "MOBILE" : field_name)).toUpperCase();
                }
                cxt.disableDefaultConstraintViolation();
                cxt.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
