package net.trellisframework.oauth.resource.keycloak.configuration;

import jakarta.servlet.DispatcherType;
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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> {
                    requests.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll();
                    for (RouteDefinition route : routes) {
                        String method = Optional.ofNullable(route.getMethod()).map(HttpMethod::name).orElse(null);
                        var matcher = method != null ? requests.requestMatchers(method, route.getAntPattern()) : requests.requestMatchers(route.getAntPattern());
                        if (route.isIgnore()) matcher.permitAll();
                        else matcher.hasAnyAuthority(route.getRoles());
                    }
                    requests.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resource -> resource.authenticationManagerResolver(resolver).bearerTokenResolver(ignoringPublicRoutes(routes)))
                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
                .build();
    }

    private static BearerTokenResolver ignoringPublicRoutes(List<RouteDefinition> routes) {
        BearerTokenResolver bearer = new DefaultBearerTokenResolver();
        List<RequestMatcher> matchers = routes.stream().filter(RouteDefinition::isIgnore).map(route -> {
            PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();
            return (RequestMatcher) (route.getMethod() != null ? builder.matcher(route.getMethod(), route.getAntPattern()) : builder.matcher(route.getAntPattern()));
        }).toList();
        if (matchers.isEmpty()) return bearer;
        RequestMatcher ignored = new OrRequestMatcher(matchers);
        return request -> ignored.matches(request) ? null : bearer.resolve(request);
    }

    @Bean
    public MultiTenancyAuthenticationManagerIssuerResolver resolver() {
        return new MultiTenancyAuthenticationManagerIssuerResolver(multiTenancyProperties);
    }
}
