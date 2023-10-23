package net.trellisframework.oauth.resource.keycloak.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.oauth.resource.keycloak.constant.Source;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.multitenancy")
public class MultiTenancyProperties {
    private boolean enabled = true;
    private Source source = Source.FILE;
    private String headerName = "X-Tenant-ID";
    private Map<String, OAuth2ResourceServerProperties.Jwt> providers;

    public Optional<OAuth2ResourceServerProperties.Jwt> findByTenantId(String tenantId) {
        return Optional.ofNullable(providers.get(tenantId));
    }
}