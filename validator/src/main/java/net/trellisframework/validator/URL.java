package net.trellisframework.validator;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import net.trellisframework.util.url.URLUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintViolationCreationContext;

import java.lang.annotation.*;
import java.text.MessageFormat;

@Documented
@Constraint(validatedBy = URL.URLValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface URL {
    String name() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class URLValidator implements ConstraintValidator<URL, String> {
        private URL annotation;

        @Override
        public void initialize(URL annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isValid(String s, ConstraintValidatorContext cxt) {
            if (!StringUtils.isEmpty(s) && !URLUtil.isValidURL(s)) {
                String message = annotation.message();
                if (StringUtils.isEmpty(message)) {
                    ConstraintViolationCreationContext constraintValidatorContext = ((ConstraintValidatorContextImpl) cxt).getConstraintViolationCreationContexts().parallelStream().findFirst().orElse(null);
                    String field_name = (StringUtils.isNotBlank(annotation.name()) ? annotation.name() : constraintValidatorContext == null ? StringUtils.EMPTY : constraintValidatorContext.getPath().getLeafNode().getName()).replaceAll("([A-Z])", "_$1");
                    message = (MessageFormat.format("{0}_IS_INVALID", StringUtils.isEmpty(field_name) ? "URL" : field_name)).toUpperCase();
                }
                cxt.disableDefaultConstraintViolation();
                cxt.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
