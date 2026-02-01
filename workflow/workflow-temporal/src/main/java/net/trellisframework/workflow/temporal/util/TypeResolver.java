package net.trellisframework.workflow.temporal.util;

import net.trellisframework.context.action.Action;
import net.trellisframework.context.task.Task;
import net.trellisframework.data.core.task.RepositoryTask;
import net.trellisframework.util.json.JsonUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


public final class TypeResolver {

    private TypeResolver() {
    }

    public static Class<?> getActivityBaseInterface(Class<?> clazz) {
        if (RepositoryTask.class.isAssignableFrom(clazz)) {
            return RepositoryTask.class;
        }
        if (Task.class.isAssignableFrom(clazz)) {
            return Task.class;
        }
        if (Action.class.isAssignableFrom(clazz)) {
            return Action.class;
        }
        throw new IllegalArgumentException("Class must implement Action, Task, or RepositoryTask: " + clazz.getName());
    }

    public static Class<?>[] getParameterTypes(Class<?> clazz, Class<?> baseInterface) {
        return resolveParameterTypes(baseInterface, clazz.getGenericInterfaces());
    }

    private static Class<?>[] resolveParameterTypes(Class<?> baseInterface, Type[] genericInterfaces) {
        for (Type type : genericInterfaces) {
            if (!(type instanceof ParameterizedType paramType)) {
                continue;
            }
            if (!(paramType.getRawType() instanceof Class<?> rawClass)) {
                continue;
            }
            if (!baseInterface.isAssignableFrom(rawClass)) {
                continue;
            }

            Type[] typeArgs = paramType.getActualTypeArguments();
            int skipCount = isRepositoryType(rawClass) ? 2 : 1;

            if (typeArgs.length > skipCount) {
                return extractParameterClasses(typeArgs, skipCount);
            }

            Class<?>[] parentResult = resolveParameterTypes(baseInterface, rawClass.getGenericInterfaces());
            if (parentResult.length > 0) {
                return parentResult;
            }
        }
        return new Class<?>[0];
    }

    private static boolean isRepositoryType(Class<?> clazz) {
        return clazz.getSimpleName().contains("Repository");
    }

    private static Class<?>[] extractParameterClasses(Type[] typeArgs, int skipCount) {
        Class<?>[] result = new Class<?>[typeArgs.length - skipCount];
        for (int i = skipCount; i < typeArgs.length; i++) {
            result[i - skipCount] = toRawClass(typeArgs[i]);
        }
        return result;
    }

    private static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> clazz) {
            return clazz;
        }
        return Object.class;
    }


    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<?> targetType) {
        if (value == null || targetType == Void.class || targetType == void.class) {
            return (T) value;
        }

        if (targetType.isInstance(value)) {
            return (T) value;
        }

        Object converted = convertCollection(value, targetType);
        if (converted != null) {
            return (T) converted;
        }

        return (T) JsonUtil.toObject(value, targetType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Type targetType) {
        if (targetType instanceof ParameterizedType pt && pt.getRawType() == java.util.Optional.class) {
            return (T) convertToOptional(value, pt);
        }
        if (value == null) {
            return null;
        }
        if (targetType instanceof Class<?> clazz) {
            return convert(value, clazz);
        }
        return (T) value;
    }

    private static Object convertCollection(Object value, Class<?> targetType) {
        if (!(value instanceof Collection<?> collection)) {
            return null;
        }

        if (Set.class.isAssignableFrom(targetType) && !(value instanceof Set)) {
            return new HashSet<>(collection);
        }

        if (List.class.isAssignableFrom(targetType) && !(value instanceof List)) {
            return new ArrayList<>(collection);
        }

        return null;
    }

    private static Object convertToOptional(Object value, ParameterizedType pt) {
        if (value == null) {
            return java.util.Optional.empty();
        }
        Type innerType = pt.getActualTypeArguments()[0];
        Class<?> innerClass = toRawClass(innerType);
        Object converted = convert(value, innerClass);
        return java.util.Optional.ofNullable(converted);
    }
}
