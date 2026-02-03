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
        Type[] types = getGenericParameterTypes(clazz, baseInterface);
        Class<?>[] result = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = toRawClass(types[i]);
        }
        return result;
    }

    public static Type[] getGenericParameterTypes(Class<?> clazz, Class<?> baseInterface) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Type[] result = resolveGenericParameterTypes(baseInterface, current.getGenericInterfaces());
            if (result.length > 0) {
                return result;
            }
            current = current.getSuperclass();
        }
        return new Type[0];
    }

    private static Type[] resolveGenericParameterTypes(Class<?> baseInterface, Type[] genericInterfaces) {
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
                return extractParameterTypes(typeArgs, skipCount);
            }

            Type[] parentResult = resolveGenericParameterTypes(baseInterface, rawClass.getGenericInterfaces());
            if (parentResult.length > 0) {
                return parentResult;
            }
        }
        return new Type[0];
    }

    private static Type[] extractParameterTypes(Type[] typeArgs, int skipCount) {
        Type[] result = new Type[typeArgs.length - skipCount];
        System.arraycopy(typeArgs, skipCount, result, 0, result.length);
        return result;
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
        if (value == null) {
            return null;
        }
        if (targetType instanceof ParameterizedType pt) {
            Type rawType = pt.getRawType();
            if (rawType == java.util.Optional.class) {
                return (T) convertToOptional(value, pt);
            }
            if (rawType instanceof Class<?> rawClass && Collection.class.isAssignableFrom(rawClass)) {
                return (T) convertToCollection(value, pt);
            }
        }
        if (targetType instanceof Class<?> clazz) {
            return convert(value, clazz);
        }
        return (T) value;
    }

    private static Object convertToCollection(Object value, ParameterizedType pt) {
        if (!(value instanceof Collection<?> collection)) {
            return value;
        }
        Type rawType = pt.getRawType();
        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length == 0) {
            return convertCollection(value, (Class<?>) rawType);
        }
        Type elementType = typeArgs[0];
        Class<?> elementClass = toRawClass(elementType);
        Class<?> collectionClass = (Class<?>) rawType;

        if (List.class.isAssignableFrom(collectionClass)) {
            return JsonUtil.toObject(collection, ArrayList.class, elementClass);
        }
        if (Set.class.isAssignableFrom(collectionClass)) {
            return JsonUtil.toObject(collection, HashSet.class, elementClass);
        }
        return JsonUtil.toObject(collection, ArrayList.class, elementClass);
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
            return Optional.empty();
        }
        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length == 0) {
            return Optional.ofNullable(value);
        }
        Type innerType = typeArgs[0];
        Class<?> innerClass = toRawClass(innerType);
        Object converted = convert(value, innerClass);
        return java.util.Optional.ofNullable(converted);
    }
}
