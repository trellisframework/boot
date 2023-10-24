package net.trellisframework.oauth.resource.keycloak.configuration;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.oauth.core.payload.RouteDefinition;
import net.trellisframework.oauth.core.payload.SpringSecurityRoutesProperties;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableMethodSecurity
@ImportAutoConfiguration(ApplicationContextProvider.class)
@EnableConfigurationProperties({SpringSecurityRoutesProperties.class, MultiTenancyProperties.class})
public class SpringSecurityConfiguration {

    private final SpringSecurityRoutesProperties properties;
    private final MultiTenancyProperties multiTenancyProperties;

    public SpringSecurityConfiguration(SpringSecurityRoutesProperties properties, MultiTenancyProperties multiTenancyProperties) {
        this.properties = properties;
        this.multiTenancyProperties = multiTenancyProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MultiTenancyAuthenticationManagerIssuerResolver resolver) throws Exception {
        List<RouteDefinition> routes = properties.getRoutes();
        for (RouteDefinition route : routes) {
            if (route.isIgnore())
                http.authorizeHttpRequests(requests -> requests.requestMatchers(new AntPathRequestMatcher(route.getAntPattern(), Optional.ofNullable(route.getMethod()).map(HttpMethod::name).orElse(null))).permitAll());
            else
                http.authorizeHttpRequests(requests -> requests.requestMatchers(new AntPathRequestMatcher(route.getAntPattern(), Optional.ofNullable(route.getMethod()).map(HttpMethod::name).orElse(null))).hasAnyAuthority(route.getRoles()));
        }
        return http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(x -> x.anyRequest().authenticated()).
                oauth2ResourceServer(resource -> resource.authenticationManagerResolver(resolver))
                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS)).build();
    }

    @Bean
    public MultiTenancyAuthenticationManagerIssuerResolver resolver() {
        return new MultiTenancyAuthenticationManagerIssuerResolver(multiTenancyProperties);
    }
}