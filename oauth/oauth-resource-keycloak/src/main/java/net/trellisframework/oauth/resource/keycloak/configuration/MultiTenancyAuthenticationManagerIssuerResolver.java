package net.trellisframework.oauth.resource.keycloak.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import net.trellisframework.oauth.resource.keycloak.action.AbstractFindTenantByIdTask;
import net.trellisframework.oauth.resource.keycloak.constant.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MultiTenancyAuthenticationManagerIssuerResolver implements AuthenticationManagerResolver<HttpServletRequest> {
    private final MultiTenancyProperties properties;
    private final ConcurrentHashMap<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    private final TenantConverter issuerConverter = new TenantConverter();
    private final AbstractFindTenantByIdTask task;

    @Autowired
    public MultiTenancyAuthenticationManagerIssuerResolver(MultiTenancyProperties properties, AbstractFindTenantByIdTask task) {
        this.properties = properties;
        this.task = task;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest context) {
        String tenantId = issuerConverter.convert(context);
        return Optional.ofNullable(task).map(x -> x.execute(tenantId)).orElse(properties.findByTenantId(tenantId))
                .map(p -> managers.computeIfAbsent(tenantId, (id) -> provider(p)::authenticate))
                .orElseThrow(() -> new InvalidBearerTokenException(Messages.UNKNOWN_TENANT.getMessage()));
    }

    private JwtAuthenticationProvider provider(OAuth2ResourceServerProperties.Jwt property) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(property.getJwkSetUri()).jwsAlgorithms(x -> x.addAll(property.getJwsAlgorithms().stream().map(SignatureAlgorithm::from).collect(Collectors.toSet()))).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(property.getIssuerUri()));
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
        authenticationProvider.setJwtAuthenticationConverter(new JwtTokenConverter());
        return authenticationProvider;
    }

    private class TenantConverter implements Converter<HttpServletRequest, String> {
        @Override
        public String convert(@NonNull HttpServletRequest context) {
            try {
                return Optional.ofNullable(context.getHeader(Optional.ofNullable(properties.getHeaderName()).orElse("X-Tenant-ID"))).orElseThrow(() -> new InvalidBearerTokenException(Messages.UNKNOWN_TENANT.getMessage()));
            } catch (Exception ex) {
                throw new InvalidBearerTokenException(ex.getMessage(), ex);
            }
        }
    }
}