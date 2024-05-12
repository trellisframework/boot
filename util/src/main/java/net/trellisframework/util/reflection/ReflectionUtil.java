package net.trellisframework.util.reflection;

import net.trellisframework.http.exception.NotFoundException;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ReflectionUtil {

    public static void setPropertyValue(Object bean, String field, Object value) {
        try {
            String[] fieldNames = field.split("\\.");
            Object currentObject = bean;

            for (int i = 0; i < fieldNames.length - 1; i++) {
                Field nestedField = currentObject.getClass().getDeclaredField(fieldNames[i]);
                nestedField.setAccessible(true);
                Object nestedObject = nestedField.get(currentObject);

                if (nestedObject == null) {
                    nestedObject = nestedField.getType().getDeclaredConstructor().newInstance();
                    nestedField.set(currentObject, nestedObject);
                }

                currentObject = nestedObject;
            }

            Field targetField = currentObject.getClass().getDeclaredField(fieldNames[fieldNames.length - 1]);
            targetField.setAccessible(true);
            targetField.set(currentObject, value);
        } catch (Exception e) {
            throw new NotFoundException("Cannot set " + field + " value");
        }
    }

    public static <T> T getPropertyValue(Object bean, String field, T defaultValue) {
        T response = getPropertyValue(bean, field);
        return Optional.ofNullable(response).orElse(defaultValue);
    }

    public static <T> T getPropertyValue(Object bean, String field) {
        try {
            Field privateField = bean.getClass().getDeclaredField(field);
            privateField.setAccessible(true);
            return (T) privateField.get(bean);
        } catch (Exception e) {
            return null;
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
