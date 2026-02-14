package net.trellisframework.core.config;

import lombok.Data;
import net.trellisframework.core.constant.ApplicationMode;
import net.trellisframework.core.constant.Country;
import net.trellisframework.core.constant.Language;
import net.trellisframework.core.payload.Payload;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("spring.application")
@Data
public class ProductionPropertiesDefinition implements Payload {
    private ApplicationMode mode = ApplicationMode.PRODUCTION;
    private Language language = Language.EN;
    private Country country = Country.US;
    private String baseUrl = "";
}