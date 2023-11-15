package net.trellisframework.oauth.resource.keycloak.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.oauth.resource.keycloak.constant.Messages;
import net.trellisframework.oauth.resource.keycloak.action.FindTenantByIdAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.ObjectUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MultiTenancyAuthenticationManagerIssuerResolver implements AuthenticationManagerResolver<HttpServletRequest> {
    private final MultiTenancyProperties properties;
    private final ConcurrentHashMap<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    private final TenantConverter issuerConverter = new TenantConverter();

    private FindTenantByIdAction action;

    public MultiTenancyAuthenticationManagerIssuerResolver(MultiTenancyProperties properties) {
        this.properties = properties;
        if (!ObjectUtils.isEmpty(ApplicationContextProvider.context.getBeansOfType(FindTenantByIdAction.class)))
            this.action = ApplicationContextProvider.context.getBean(FindTenantByIdAction.class);
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest context) {
        String tenantId = issuerConverter.convert(context);
        if (StringUtils.isBlank(tenantId))
            throw new InvalidBearerTokenException(Messages.UNKNOWN_ISSUER.getMessage());
        AuthenticationManager manager = managers.get(tenantId);
        if (manager == null) {
            OAuth2ResourceServerProperties.Jwt jwt = (action != null ? action.execute(context) : properties.findByTenantId(tenantId)).orElseThrow(() -> new InvalidBearerTokenException(Messages.UNKNOWN_ISSUER.getMessage()));
            manager = provider(jwt)::authenticate;
            managers.put(tenantId, manager);
        }
        return manager;
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
                return Optional.ofNullable(context.getHeader(Optional.ofNullable(properties.getHeaderName()).orElse("X-Tenant-ID"))).orElseThrow(() -> new InvalidBearerTokenException(Messages.UNKNOWN_ISSUER.getMessage()));
            } catch (Exception ex) {
                throw new InvalidBearerTokenException(ex.getMessage(), ex);
            }
        }
    }
}