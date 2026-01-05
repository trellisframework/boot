package net.trellisframework.ui.web.adapter;

import net.trellisframework.core.log.Logger;
import net.trellisframework.ui.web.bind.RequestHeader;
import net.trellisframework.ui.web.helper.RequestHelper;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.util.reflection.ReflectionUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class RequestHeaderInjectorAdviceAdapter extends RequestBodyAdviceAdapter {
    static ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(@NotNull MethodParameter methodParameter, @NotNull Type targetType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        try {
            if (JacksonJsonHttpMessageConverter.class.isAssignableFrom(converterType) && targetType instanceof Class) {
                for (Field field : ((Class<?>) targetType).getDeclaredFields()) {
                    if (field.isAnnotationPresent(RequestHeader.class)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Logger.error("InjectHeaderException", e.getMessage());
            return false;
        }
    }

    @NotNull
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, @NotNull MethodParameter methodParameter, @NotNull Type targetType, @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType) throws IOException {
        String bodyStr = IOUtils.toString(new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8));
        try {
            var instance = JsonUtil.toObject(bodyStr, mapper.getTypeFactory().constructType(targetType));
            Class<?> target = ((Class<?>) targetType);
            for (Field field : target.getDeclaredFields()) {
                if (field.isAnnotationPresent(RequestHeader.class)) {
                    RequestHeader property = field.getAnnotation(RequestHeader.class);
                    String value = StringUtils.isBlank(property.value()) ? field.getName() : property.value();
                    ReflectionUtil.setPropertyValue(instance, StringUtils.isBlank(property.target()) ? field.getName() : property.target(), RequestHelper.getHeaderValue(value));
                }
            }
            return new SimpleHttpInputMessage(IOUtils.toInputStream(JsonUtil.toString(instance), StandardCharsets.UTF_8), inputMessage.getHeaders());
        } catch (Exception e) {
            Logger.error("InjectHeaderException", e.getMessage());
            return new SimpleHttpInputMessage(IOUtils.toInputStream(bodyStr, StandardCharsets.UTF_8), inputMessage.getHeaders());
        }
    }
}