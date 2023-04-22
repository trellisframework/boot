package net.trellisframework.oauth.resource.keycloak.helper;

import net.trellisframework.context.payload.Permission;
import net.trellisframework.context.payload.Principle;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.log.Logger;
import net.trellisframework.oauth.resource.keycloak.payload.Token;
import net.trellisframework.util.mapper.ModelMapper;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Date;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OAuthSecurityContext implements ModelMapper {

    private static AdapterConfig adapterConfig;

    private static KeycloakDeployment keycloakDeployment;

    static AdapterConfig getAdapterConfig() {
        if (adapterConfig == null)
            adapterConfig = ApplicationContextProvider.context.getBean(AdapterConfig.class);
        return adapterConfig;
    }

    static KeycloakDeployment getKeycloakDeployment() {
        if (keycloakDeployment == null)
            keycloakDeployment = KeycloakDeploymentBuilder.build(getAdapterConfig());
        return keycloakDeployment;
    }

    public static Optional<Principle> findPrincipleByAccessToken(String token) {
        try {
            KeycloakDeployment deployment = getKeycloakDeployment();
            AccessToken accessToken = AdapterTokenVerifier.verifyToken(token, deployment);
            Set<Permission> permissions = Optional.of(accessToken).map(AccessToken::getAuthorization).map(AccessToken.Authorization::getPermissions).isPresent() ? accessToken.getAuthorization().getPermissions().stream().map(x -> Permission.of(x.getResourceId(), x.getResourceName(), x.getScopes(), x.getClaims())).collect(Collectors.toSet()) : new HashSet<>();
            return Optional.of(Principle.of(accessToken.getSubject(), accessToken.getEmail(), accessToken.getPreferredUsername(), accessToken.getGivenName(), accessToken.getFamilyName(), accessToken.getOtherClaims(), accessToken.getResourceAccess(), permissions));
        } catch (Exception e) {
            Logger.error("extractPrincipleByAccessToken", "token: "+ token + " message: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Principle getAuthorizationPrinciple() {
        KeycloakPrincipal securityContext = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthzClient authzClient = AuthzClient.create(new Configuration(getAdapterConfig().getAuthServerUrl(), getAdapterConfig().getRealm(), getAdapterConfig().getResource(), getKeycloakDeployment().getResourceCredentials(), getKeycloakDeployment().getClient()));
        AuthorizationResponse authorization = authzClient.authorization(securityContext.getKeycloakSecurityContext().getTokenString()).authorize(new AuthorizationRequest());
        return OAuthSecurityContext.findPrincipleByAccessToken(authorization.getToken()).orElse(null);
    }

    public static Principle getPrinciple() {
        KeycloakPrincipal securityContext = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessToken token = securityContext.getKeycloakSecurityContext().getToken();
        return token == null ? null : Principle.of(token.getSubject(), token.getEmail(), token.getPreferredUsername(), token.getGivenName(), token.getFamilyName(), token.getOtherClaims(), token.getResourceAccess(), null);
    }

    public static String getPrincipleId() {
        Principle principle = getPrinciple();
        return principle == null ? StringUtils.EMPTY : principle.getId();
    }

    public static Token getAccessToken() {
        KeycloakPrincipal securityContext = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessToken token = securityContext.getKeycloakSecurityContext().getToken();
        return new Token(token.getId(), securityContext.getKeycloakSecurityContext().getTokenString(), token.getScope(), Date.from(Instant.ofEpochSecond(token.getExp())), token.isExpired());
    }

    public static boolean hasRole(String role) {
        return hasAnyRole(role);
    }

    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (roles == null || authentication == null)
            return false;
        for (String role : roles) {
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_" + role)))
                return true;
        }
        return false;
    }

}
