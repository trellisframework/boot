package net.trellisframework.core;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.constant.Country;
import net.trellisframework.core.constant.Language;
import net.trellisframework.core.constant.ProductionMode;
import net.trellisframework.core.config.ProductionPropertiesDefinition;

import java.util.Optional;

public class AppInfo {
    static ProductionPropertiesDefinition properties;
    static ProductionMode mode;
    static Language language;
    static Country country;

    public static boolean isProductionMode() {
        try {
            mode = Optional.ofNullable(mode).orElse(getProperties().getMode());
        } catch (Exception e) {
            mode = ProductionMode.PRODUCTION;
        }
        return ProductionMode.PRODUCTION.equals(mode);
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
