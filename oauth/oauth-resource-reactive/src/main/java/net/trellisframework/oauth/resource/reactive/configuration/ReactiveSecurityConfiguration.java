package net.trellisframework.oauth.resource.reactive.configuration;

import net.trellisframework.oauth.core.payload.RouteDefinition;
import net.trellisframework.oauth.core.payload.SpringSecurityRoutesProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SpringSecurityRoutesProperties.class)
public class ReactiveSecurityConfiguration {

    private final SpringSecurityRoutesProperties properties;

    public ReactiveSecurityConfiguration(SpringSecurityRoutesProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        List<RouteDefinition> routes = properties.getRoutes();
        for (RouteDefinition route : routes) {
            if (route.isIgnore())
                http.authorizeExchange().pathMatchers(route.getMethod(), route.getAntPattern()).permitAll();
            else
                http.authorizeExchange().pathMatchers(route.getMethod(), route.getAntPattern()).hasAnyAuthority(route.getRoles());
        }
        http.cors(cors -> cors.configurationSource(cors())).csrf().disable().authorizeExchange().anyExchange().authenticated().and().oauth2ResourceServer().jwt();
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.getJwt().getJwkSetUri()).jwsAlgorithm(SignatureAlgorithm.from(properties.getJwt().getJwsAlgorithm())).build();
        if (properties.getJwt().getIssuerUri() != null) {
            List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
            validators.add(new JwtTimestampValidator(Duration.ZERO));
            validators.add(new JwtIssuerValidator(properties.getJwt().getIssuerUri()));
            jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        }
        return jwtDecoder;
    }

    private CorsConfigurationSource cors() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}