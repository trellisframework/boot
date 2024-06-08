package net.trellisframework.util.object;

import org.apache.commons.lang3.ObjectUtils;

public class ObjectUtil {

    public static boolean isAnyNull(Object... objects) {
        if (objects == null || objects.length == 0)
            return true;
        for (Object object : objects) {
            if (object == null)
                return true;
        }
        return false;
    }

    public static boolean isAllNull(Object... objects) {
        if (objects == null)
            return true;
        for (Object object : objects) {
            if (object != null)
                return false;
        }
        return true;
    }

    public static boolean isNoneNull(Object... objects) {
        return !isAnyNull(objects);
    }

    public static <T> T emptyToNull(T object) {
        return ObjectUtils.isEmpty(object) ? null : object;
    }

    public static <T> T nullIfEmpty(T object) {
        return defaultIfEmpty(object, null);
    }

    public static <T> T defaultIfEmpty(T object, T defaultValue) {
        return ObjectUtils.isNotEmpty(object) ? object : defaultValue;
    }

}
