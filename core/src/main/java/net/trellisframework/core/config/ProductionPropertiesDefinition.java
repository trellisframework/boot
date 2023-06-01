package net.trellisframework.core.config;

import net.trellisframework.core.constant.Country;
import net.trellisframework.core.constant.Language;
import net.trellisframework.core.constant.ProductionMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Payload;

@ConfigurationProperties("spring.application")
@Validated
@Data
public class ProductionPropertiesDefinition implements Payload {
    private ProductionMode mode = ProductionMode.PRODUCTION;
    private Language language = Language.EN;
    private Country country = Country.US;
    private String baseUrl = "";
}