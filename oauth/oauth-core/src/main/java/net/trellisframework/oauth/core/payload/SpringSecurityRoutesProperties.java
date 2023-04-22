package net.trellisframework.oauth.core.payload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("spring.security")
@Validated
@Data
public class SpringSecurityRoutesProperties {

    private List<RouteDefinition> routes = new ArrayList<>();

}