package net.trellisframework.oauth.core.payload;

import lombok.Data;
import org.springframework.http.HttpMethod;

@Data
public class RouteDefinition {
    private String antPattern;

    private HttpMethod method;

    private boolean ignore = false;

    private String[] roles;
}
