package net.trellisframework.oauth.resource.keycloak.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver")
public class MultiTenancyProperties {
    private boolean enabled = true;
    private String headerName = "X-Tenant-ID";
    private Map<String, OAuth2ResourceServerProperties.Jwt> issuers;

    public Optional<OAuth2ResourceServerProperties.Jwt> findByTenantId(String tenantId) {
        return Optional.ofNullable(issuers).map(x -> x.get(tenantId));
    }

    public Map.Entry<String, OAuth2ResourceServerProperties.Jwt> getFirst() {
        return Optional.ofNullable(issuers).map(Map::entrySet).orElse(new HashSet<>()).stream().findFirst().orElse(null);
    }
}