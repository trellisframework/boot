package net.trellisframework.core.message;

import net.trellisframework.core.AppInfo;
import net.trellisframework.core.constant.Language;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageHelper {
    public static String getMessage(String message) {
        return getMessage(AppInfo.getLanguage(), message);
    }

    public static Integer getCode(String message) {
        try {
            Locale locale = Locale.of("no");
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            return Integer.valueOf(bundle.getString(message));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getMessage(Language language, String message) {
        try {
            language = language == null ? AppInfo.getLanguage() : language;
            Locale locale = Locale.of(language.name());
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            return bundle.getString(message).trim();
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    public static String getMessage(String message, Object... var1) {
        return getMessage(AppInfo.getLanguage(), message, var1);
    }

    public static String getMessage(Language language, String message, Object... var1) {
        try {
            language = language == null ? AppInfo.getLanguage() : language;
            Locale locale = Locale.of(language.name());
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            return MessageFormat.format(bundle.getString(message), var1).trim();
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }
}
