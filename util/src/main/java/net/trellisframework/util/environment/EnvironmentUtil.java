package net.trellisframework.util.environment;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

public class EnvironmentUtil {

    public static String getPropertyValue(String property, String defaultValue) {
        try {
            Environment environment = ApplicationContextProvider.context.getBean(Environment.class);
            String value = environment.getProperty(property, defaultValue);
            return (StringUtils.isBlank(value)) ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
