package net.trellisframework.workflow.temporal.util;

import net.trellisframework.util.json.JsonUtil;

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

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<?> targetType) {
        if (value == null || targetType == Void.class || targetType == void.class || targetType.isInstance(value)) {
            return (T) value;
        }
        return (T) JsonUtil.toObject(value, targetType);
    }

}


