package net.trellisframework.core;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.constant.Country;
import net.trellisframework.core.constant.Language;
import net.trellisframework.core.constant.ApplicationMode;
import net.trellisframework.core.config.ProductionPropertiesDefinition;

import java.util.Optional;

public class AppInfo {
    static ProductionPropertiesDefinition properties;
    static ApplicationMode mode;
    static Language language;
    static Country country;

    public static boolean isProductionMode() {
        return ApplicationMode.PRODUCTION.equals(getApplicationMode());
    }

    public static ApplicationMode getApplicationMode() {
        if (mode == null) {
            mode = Optional.ofNullable(getProperties().getMode()).orElse(ApplicationMode.PRODUCTION);
        }
        return mode;
    }

    public static Language getLanguage() {
        try {
            language = Optional.ofNullable(language).orElse(getProperties().getLanguage());
        } catch (Exception e) {
            language = Language.EN;
        }
        return language;
    }

    public static Country getCountry() {
        try {
            country = Optional.ofNullable(country).orElse(getProperties().getCountry());
        } catch (Exception e) {
            country = Country.US;
        }
        return country;
    }

    public static ProductionPropertiesDefinition getProperties() {
        if (properties == null)
            properties = ApplicationContextProvider.context.getBean(ProductionPropertiesDefinition.class);
        return properties;
    }
}
