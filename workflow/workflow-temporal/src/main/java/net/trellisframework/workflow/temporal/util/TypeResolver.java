package net.trellisframework.workflow.temporal.util;

import net.trellisframework.context.action.Action;
import net.trellisframework.context.task.Task;
import net.trellisframework.data.core.task.RepositoryTask;
import net.trellisframework.util.json.JsonUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class TypeResolver {

    private TypeResolver() {
    }

    public static Class<?> getActivityBaseInterface(Class<?> clazz) {
        if (RepositoryTask.class.isAssignableFrom(clazz)) {
            return RepositoryTask.class;
        } else if (Task.class.isAssignableFrom(clazz)) {
            return Task.class;
        } else if (Action.class.isAssignableFrom(clazz)) {
            return Action.class;
        }
        throw new IllegalArgumentException("Class must implement Action, Task, or RepositoryTask: " + clazz.getName());
    }

    public static Class<?>[] getParameterTypes(Class<?> clazz, Class<?> baseInterface) {
        return getParameterTypesRecursive(clazz, baseInterface, clazz.getGenericInterfaces());
    }

    private static Class<?>[] getParameterTypesRecursive(Class<?> originalClass, Class<?> baseInterface, Type[] genericInterfaces) {
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rawClass) {
                if (baseInterface.isAssignableFrom(rawClass)) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    // For RepositoryTask: <R, O, I1, I2...> - skip R and O (first 2)
                    // For Task/Action: <O, I1, I2...> - skip O (first 1)
                    int skipCount = rawClass.getSimpleName().contains("Repository") ? 2 : 1;
                    if (typeArgs.length > skipCount) {
                        Class<?>[] paramTypes = new Class<?>[typeArgs.length - skipCount];
                        for (int i = skipCount; i < typeArgs.length; i++) {
                            paramTypes[i - skipCount] = typeArgs[i] instanceof Class<?> c ? c : Object.class;
                        }
                        return paramTypes;
                    }
                    // If this interface doesn't have enough type args, check its parent interfaces
                    Class<?>[] result = getParameterTypesRecursive(originalClass, baseInterface, rawClass.getGenericInterfaces());
                    if (result.length > 0) {
                        return result;
                    }
                }
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

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Type targetType) {
        if (targetType instanceof ParameterizedType pt && pt.getRawType() == java.util.Optional.class) {
            if (value == null) {
                return (T) java.util.Optional.empty();
            }
            Type innerType = pt.getActualTypeArguments()[0];
            Class<?> innerClass = innerType instanceof Class<?> c ? c : Object.class;
            Object converted = convert(value, innerClass);
            return (T) java.util.Optional.ofNullable(converted);
        }
        if (value == null) {
            return (T) value;
        }
        if (targetType instanceof Class<?> clazz) {
            return convert(value, clazz);
        }
        return (T) value;
    }

}


