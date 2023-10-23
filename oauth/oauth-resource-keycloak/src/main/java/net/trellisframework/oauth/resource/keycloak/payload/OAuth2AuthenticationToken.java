package net.trellisframework.oauth.resource.keycloak.payload;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public class OAuth2AuthenticationToken extends AbstractAuthenticationToken {
    private final Jwt jwt;
    private final String name;


    public OAuth2AuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name) {
        super(authorities);
        this.jwt = jwt;
        this.name = name;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return jwt;
    }

    @Override
    public String getName() {
        return name;
    }
}