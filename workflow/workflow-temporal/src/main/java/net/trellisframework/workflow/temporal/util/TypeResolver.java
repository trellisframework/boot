package net.trellisframework.workflow.temporal.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.trellisframework.core.application.ApplicationContextProvider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class TypeResolver {

    private TypeResolver() {
    }

    public static Class<?>[] getParameterTypes(Class<?> clazz, Class<?> baseInterface) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rawClass && baseInterface.isAssignableFrom(rawClass)) {
                Type[] typeArgs = pt.getActualTypeArguments();
                Class<?>[] paramTypes = new Class<?>[typeArgs.length - 1];
                for (int i = 1; i < typeArgs.length; i++) {
                    paramTypes[i - 1] = typeArgs[i] instanceof Class<?> c ? c : Object.class;
                }
                return paramTypes;
            }
        }
        return new Class<?>[0];
    }

    public static Class<?> getReturnType(Class<?> clazz, Class<?> baseInterface) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rawClass && baseInterface.isAssignableFrom(rawClass)) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                    return c;
                }
            }
        }
        return Object.class;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return (T) value;
        }
        ObjectMapper mapper = ApplicationContextProvider.context.getBean(ObjectMapper.class);
        return (T) mapper.convertValue(value, targetType);
    }
}
