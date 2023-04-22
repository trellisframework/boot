package net.trellisframework.oauth.resource.configuration;

import net.trellisframework.oauth.core.payload.RouteDefinition;
import net.trellisframework.oauth.core.payload.SpringSecurityRoutesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.List;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SpringSecurityRoutesProperties.class)
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final SpringSecurityRoutesProperties properties;

    public SpringSecurityConfiguration(SpringSecurityRoutesProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        List<RouteDefinition> routes = properties.getRoutes();
        for (RouteDefinition route : routes) {
            if (route.isIgnore())
                http.authorizeRequests().mvcMatchers(route.getMethod(), route.getAntPattern()).permitAll();
            else
                http.authorizeRequests().mvcMatchers(route.getMethod(), route.getAntPattern()).hasAnyAuthority(route.getRoles());
        }
        http.csrf().disable().cors().disable().authorizeRequests().anyRequest().authenticated();
    }
}