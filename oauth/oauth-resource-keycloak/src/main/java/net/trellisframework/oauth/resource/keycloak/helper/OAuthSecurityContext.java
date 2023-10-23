package net.trellisframework.oauth.resource.keycloak.helper;

import net.trellisframework.context.payload.Principle;
import net.trellisframework.oauth.resource.keycloak.constant.KeycloakClaimNames;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OAuthSecurityContext {

    public static Principle getPrinciple() {
        Jwt jwt = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return Optional.ofNullable(jwt)
                .map(x -> Principle.of(
                        x.getClaim(KeycloakClaimNames.SUB.getName()),
                        x.getClaim(KeycloakClaimNames.EMAIL.getName()),
                        x.hasClaim(KeycloakClaimNames.PREFERRED_USERNAME.getName()) ? x.getClaimAsString(KeycloakClaimNames.PREFERRED_USERNAME.getName()) : x.getSubject(),
                        x.getClaim(KeycloakClaimNames.GIVEN_NAME.getName()),
                        x.getClaim(KeycloakClaimNames.FAMILY_NAME.getName()),
                        x.getClaims().entrySet().stream().filter(entry -> !KeycloakClaimNames.names().contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                        new HashMap<>(),
                        new ArrayList<>()
                )).orElse(Principle.of());
    }

    public static String getPrincipleId() {
        Principle principle = getPrinciple();
        return principle == null ? StringUtils.EMPTY : principle.getId();
    }
}
