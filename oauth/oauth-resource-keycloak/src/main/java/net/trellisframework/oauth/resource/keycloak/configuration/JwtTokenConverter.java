package net.trellisframework.oauth.resource.keycloak.configuration;

import jakarta.validation.constraints.NotNull;
import net.trellisframework.oauth.resource.keycloak.constant.KeycloakClaimNames;
import net.trellisframework.oauth.resource.keycloak.payload.OAuth2AuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class JwtTokenConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Converter<Jwt, Collection<GrantedAuthority>> JWT_SCOPE_GRANTED_AUTHORITIES_CONVERTER = new JwtGrantedAuthoritiesConverter();
    private static final String DEFAULT_PREFIX = "ROLE_";
    private String PREFIX = DEFAULT_PREFIX;

    @Override
    public AbstractAuthenticationToken convert(@NotNull Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(getRealmRolesFrom(jwt));
        authorities.addAll(getClientRole(jwt));
        authorities.addAll(getScopes(jwt));
        return new OAuth2AuthenticationToken(jwt, authorities, jwt.hasClaim(KeycloakClaimNames.PREFERRED_USERNAME.getName()) ? jwt.getClaimAsString(KeycloakClaimNames.PREFERRED_USERNAME.getName()) : jwt.getSubject());
    }

    protected Set<GrantedAuthority> getRealmRolesFrom(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(KeycloakClaimNames.REALM_ACCESS.getName());
        if (CollectionUtils.isEmpty(realmAccess)) {
            return Collections.emptySet();
        }
        @SuppressWarnings("unchecked")
        Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
        if (CollectionUtils.isEmpty(realmRoles)) {
            return Collections.emptySet();
        }
        return realmRoles.stream().map(x -> x.replace('-', '_')).map(x -> new SimpleGrantedAuthority(this.PREFIX + x)).collect(Collectors.toSet());
    }

    public Set<GrantedAuthority> getClientRole(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(KeycloakClaimNames.RESOURCE_ACCESS.getName());
        if (CollectionUtils.isEmpty(resourceAccess)) {
            return Collections.emptySet();
        }
        Set<String> roles = resourceAccess.values().stream()
                .filter(access -> access instanceof Map)
                .flatMap(access -> ((Map<String, List<String>>) access).getOrDefault("roles", Collections.emptyList()).stream())
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptySet();
        }

        return roles.stream().map(x -> x.replace('-', '_')).map(x -> new SimpleGrantedAuthority(this.PREFIX + x)).collect(Collectors.toSet());
    }

    protected List<GrantedAuthority> getScopes(Jwt jwt) {
        Collection<GrantedAuthority> scopeAuthorities = JWT_SCOPE_GRANTED_AUTHORITIES_CONVERTER.convert(jwt);
        return ObjectUtils.isEmpty(scopeAuthorities) ? new ArrayList<>() : scopeAuthorities.stream().toList();
    }
}