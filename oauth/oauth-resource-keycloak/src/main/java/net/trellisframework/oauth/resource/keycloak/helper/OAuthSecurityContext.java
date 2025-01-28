package net.trellisframework.oauth.resource.keycloak.helper;

import net.trellisframework.context.payload.Principle;
import net.trellisframework.core.log.Logger;
import net.trellisframework.oauth.resource.keycloak.constant.KeycloakClaimNames;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OAuthSecurityContext {


    public static Principle getPrinciple() {
        Jwt jwt = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return Optional.ofNullable(jwt).map(OAuthSecurityContext::getPrinciple).orElse(Principle.of());
    }

    public static String getPrincipleId() {
        Principle principle = getPrinciple();
        return principle == null ? StringUtils.EMPTY : principle.getId();
    }

    public static Optional<Principle> findPrincipleByAccessToken(OAuth2ResourceServerProperties.Jwt jwt, String token) {
        try {
            return Optional.ofNullable(JwtDecoders.fromIssuerLocation(jwt.getIssuerUri()).decode(token)).map(OAuthSecurityContext::getPrinciple);
        } catch (Exception e) {
            Logger.error("JwtDecodeException", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static Principle getPrinciple(Jwt jwt) {
        return Principle.of(
                jwt.getClaim(KeycloakClaimNames.SUB.getName()),
                jwt.getClaim(KeycloakClaimNames.EMAIL.getName()),
                jwt.hasClaim(KeycloakClaimNames.PREFERRED_USERNAME.getName()) ? jwt.getClaimAsString(KeycloakClaimNames.PREFERRED_USERNAME.getName()) : jwt.getSubject(),
                jwt.getClaim(KeycloakClaimNames.GIVEN_NAME.getName()),
                jwt.getClaim(KeycloakClaimNames.FAMILY_NAME.getName()),
                jwt.getClaims().entrySet().stream().filter(entry -> !KeycloakClaimNames.names().contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                new HashMap<>(),
                new ArrayList<>()
        );
    }
}
