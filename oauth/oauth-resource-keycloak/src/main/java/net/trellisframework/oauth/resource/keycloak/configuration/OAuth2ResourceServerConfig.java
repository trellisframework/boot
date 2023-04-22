package net.trellisframework.oauth.resource.keycloak.configuration;

import net.trellisframework.oauth.core.payload.RouteDefinition;
import net.trellisframework.oauth.core.payload.SpringSecurityRoutesProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.security.Security;
import java.util.List;

@Configuration
@KeycloakConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SpringSecurityRoutesProperties.class)
public class OAuth2ResourceServerConfig extends KeycloakWebSecurityConfigurerAdapter {

    private final SpringSecurityRoutesProperties properties;

    public OAuth2ResourceServerConfig(SpringSecurityRoutesProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) {
        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        super.configure(http);
        http.exceptionHandling().defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/**"));
        List<RouteDefinition> routes = properties.getRoutes();
        for (RouteDefinition route : routes) {
            if (route.isIgnore())
                http.authorizeRequests().mvcMatchers(route.getMethod(), route.getAntPattern()).permitAll();
            else
                http.authorizeRequests().mvcMatchers(route.getMethod(), route.getAntPattern()).hasAnyAuthority(route.getRoles());
        }
        http.csrf().disable().authorizeRequests().anyRequest().authenticated();
    }

    @Bean
    @Primary
    public KeycloakConfigResolver keycloakConfigResolver(KeycloakSpringBootProperties properties) {
        return new KeycloakConfigurationResolver(properties);
    }
}