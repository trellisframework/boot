package net.trellisframework.ui.web.adapter;

import net.trellisframework.http.exception.HttpException;
import net.trellisframework.ui.web.bind.RequestParams;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Component
public class RequestParamsAdviceAdapter implements HandlerMethodArgumentResolver {
    private final Validator validator;

    public RequestParamsAdviceAdapter(Validator validator) {
        this.validator = validator;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestParams.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Class<?> parameterType = parameter.getParameterType();
        Map<String, String[]> parameterMap = webRequest.getParameterMap();
        try {
            Constructor<?> constructor = parameterType.getDeclaredConstructor();
            Object instance = constructor.newInstance();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String fieldName = entry.getKey();
                String[] fieldValues = entry.getValue();
                String fieldValue = (fieldValues != null && fieldValues.length > 0) ? fieldValues[0] : null;
                java.lang.reflect.Field field = parameterType.getDeclaredField(fieldName);
                field.setAccessible(true);
                if (fieldValue != null) {
                    if (field.getType().equals(Integer.class)) {
                        field.set(instance, Integer.parseInt(fieldValue));
                    } else if (field.getType().equals(Long.class)) {
                        field.set(instance, Long.parseLong(fieldValue));
                    } else if (field.getType().equals(Double.class)) {
                        field.set(instance, Double.parseDouble(fieldValue));
                    } else if (field.getType().equals(BigDecimal.class)) {
                        field.set(instance, new BigDecimal(fieldValue));
                    } else if (field.getType().equals(Date.class)) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        field.set(instance, dateFormat.parse(fieldValue));
                    } else {
                        field.set(instance, fieldValue);
                    }
                }
            }
            Errors errors = new BeanPropertyBindingResult(instance, "queryParameters");
            validator.validate(instance, errors);
            if (errors.hasErrors()) {
                Optional<ObjectError> ex = errors.getAllErrors().parallelStream().findFirst();
                if (ex.isPresent()) {
                    throw new HttpException(ex.map(DefaultMessageSourceResolvable::getDefaultMessage).orElse(StringUtils.EMPTY), StringUtils.isNumeric(ex.get().getCode()) ? HttpStatus.resolve(Integer.parseInt(ex.get().getCode())) : HttpStatus.BAD_REQUEST);
                }
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException | NumberFormatException |
                 NoSuchMethodException | InvocationTargetException | ParseException e) {
            throw new RuntimeException("Error resolving argument", e);
        }
    }
}
