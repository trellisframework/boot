package net.trellisframework.context.rule;

import net.trellisframework.context.payload.DiscoveryRule;
import net.trellisframework.util.reflection.ReflectionUtil;
import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import net.trellisframework.core.log.Logger;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


@Documented
@Constraint(validatedBy = Rules.RuleValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Rules {
    String message() default "";

    Class<? extends AbstractRule<?>>[] value() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class RuleValidator implements ConstraintValidator<Rules, Object> {
        private Rules annotation;

        @Override
        public void initialize(Rules annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean isValid(Object field, ConstraintValidatorContext cxt) {
            DiscoveryRule discovery = getAllRules(cxt);
            if (discovery.isSuccess()) {
                for (AbstractRule<?> instance : discovery.getRules()) {
                    boolean isSuccess;
                    if (instance instanceof DerivationRule)
                        isSuccess = fireDerivationRule((DerivationRule<Object>) instance, field, cxt);
                    else
                        isSuccess = fireConstraintRule((ConstraintRule<Object>) instance, field, cxt);
                    if (!isSuccess)
                        return false;
                }
                return true;
            }
            return false;
        }

        public DiscoveryRule getAllRules(ConstraintValidatorContext cxt) {
            if (ObjectUtils.isEmpty(Optional.ofNullable(annotation).map(Rules::value)))
                return DiscoveryRule.of(true);
            List<AbstractRule<?>> instances = new ArrayList<>();
            for (Class<? extends AbstractRule<?>> rule : annotation.value()) {
                AbstractRule<?> instance = getInstance(rule, cxt);
                if (ObjectUtils.isEmpty(instance))
                    return DiscoveryRule.of(false);
                if (instances.stream().map(x -> x.getClass().getSimpleName()).noneMatch(x -> x.equalsIgnoreCase(instance.getClass().getSimpleName())))
                    instances.add(instance);
            }
            return DiscoveryRule.of(instances);
        }

        private boolean fireDerivationRule(DerivationRule<Object> instance, Object field, ConstraintValidatorContext cxt) {
            try {
                if (!instance.isEnable() && instance.getFields() == null || instance.getFields().isEmpty())
                    return true;
                for (String fieldName : instance.getFields()) {
                    if (instance.condition(field))
                        ReflectionUtil.setPropertyValue(field, fieldName, instance.getDerivedValue(field));
                }
            } catch (Throwable e) {
                Logger.error("ValidationDerivationRulesException", "Rule: " + instance.getClass().getSimpleName() + " Message: " + e.getMessage());
                return parseErrorMessage(e, instance.getClass().getSimpleName(), cxt);
            }
            return true;
        }

        private boolean fireConstraintRule(ConstraintRule<Object> instance, Object field, ConstraintValidatorContext cxt) {
            try {
                if (instance.isEnable() && instance.condition(field)) {
                    String message = instance.message(field);
                    if (StringUtils.isEmpty(message)) {
                        String rule_name = StringUtils.uncapitalize(instance.getClass().getSimpleName()).replaceAll("([A-Z])", "_$1");
                        message = (rule_name.toUpperCase() + "_FAILED").trim();
                    }
                    cxt.disableDefaultConstraintViolation();
                    cxt.buildConstraintViolationWithTemplate(new HttpException(new HttpErrorMessage(instance.httpStatus(), message)).toString()).addConstraintViolation();
                    return false;
                }
                return true;
            } catch (Throwable e) {
                Logger.error("ValidationConstraintRulesException", "Rule: " + instance.getClass().getSimpleName() + " Message: " + e.getMessage());
                return parseErrorMessage(e, instance.getClass().getSimpleName(), cxt);
            }
        }

        private boolean parseErrorMessage(Throwable e, String ruleName, ConstraintValidatorContext cxt) {
            String message = StringUtils.EMPTY;
            if (e instanceof HttpException) {
                message = e.toString();
            }
            if (StringUtils.isBlank(message)) {
                String rule_name = StringUtils.uncapitalize(ruleName).replaceAll("([A-Z])", "_$1");
                message = new ServiceUnavailableException((rule_name.toUpperCase() + "_STRUCTURE_HAS_ERROR").trim()).toString();
            }
            cxt.disableDefaultConstraintViolation();
            cxt.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            return false;
        }

        private void parseStructureErrorMessage(String ruleName, ConstraintValidatorContext cxt) {
            String rule_name = StringUtils.uncapitalize(ruleName).replaceAll("([A-Z])", "_$1");
            String message = (rule_name.toUpperCase() + "_STRUCTURE_HAS_ERROR").trim();
            cxt.disableDefaultConstraintViolation();
            cxt.buildConstraintViolationWithTemplate(new ServiceUnavailableException(message).toString()).addConstraintViolation();
        }

        private <T extends AbstractRule<?>> T getInstance(Class<T> rule, ConstraintValidatorContext cxt) {
            try {
                return rule.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                Logger.error("GetInstanceDerivationRulesException", "Rule: " + rule.getSimpleName() + " Message: " + e.getMessage());
                parseStructureErrorMessage(rule.getSimpleName(), cxt);
                return null;
            }
        }
    }
}
