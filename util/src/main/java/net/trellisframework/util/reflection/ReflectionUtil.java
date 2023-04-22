package net.trellisframework.util.reflection;

import de.cronn.reflection.util.PropertyGetter;
import de.cronn.reflection.util.PropertyUtils;
import de.cronn.reflection.util.TypedPropertyGetter;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReflectionUtil {

    public static <T> Set<String> getPropertiesName(Class<T> clazz, PropertyGetter<T>... properties) {
        return getPropertiesName(clazz, Set.of(properties));
    }

    public static <T> Set<String> getPropertiesName(Class<T> clazz, Set<PropertyGetter<T>> properties) {
        Set<String> result = new HashSet<>();
        if (properties == null || properties.size() <= 0 || clazz == null)
            return result;
        for (TypedPropertyGetter<T, ?> property : properties) {
            result.add(PropertyUtils.getPropertyName(clazz, property));
        }
        return result;
    }

    public static void setPropertyValue(Object bean, String name, Object value) throws NoSuchMethodException {
        try {
            if (name.contains(".")) {
                int firstDotLocation = name.indexOf('.');
                String childFieldName = name.substring(0, firstDotLocation);
                Field field = bean.getClass().getDeclaredField(childFieldName);
                field.setAccessible(true);
                Object childFieldInstance = field.get(bean);
                if (childFieldInstance == null) {
                    Class<?> type = field.getType();
                    childFieldInstance = type.getConstructor().newInstance();
                    field.set(bean, childFieldInstance);
                }
                field.setAccessible(false);
                setPropertyValue(childFieldInstance, name.substring(firstDotLocation + 1), value);
            } else {

                Map<String, String> fields = BeanUtils.describe(bean);
                if (!fields.containsKey(name))
                    throw new NoSuchMethodException(name + " does not exist");
                BeanUtils.setProperty(bean, name, value);

            }
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | NoSuchFieldException | InstantiationException e) {
            throw new NoSuchMethodException("Cannot set: " + name);
        }
    }

    public static Object getPropertyValue(Object bean, String name) throws NoSuchMethodException {
        try {
            Map<String, String> fields = BeanUtils.describe(bean);
            if (!fields.containsKey(name))
                throw new NoSuchMethodException(name + " does not exist");
            return BeanUtils.getProperty(bean, name);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new NoSuchMethodException("Cannot get: " + name);
        }
    }

    public static Set<Field> getFields(Class<?> clazz) {
        Set<Field> fields = new HashSet<>();
        while (clazz != null) {
            try {
                fields.addAll(Set.of(clazz.getDeclaredFields()));
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
