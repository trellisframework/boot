package net.trellisframework.oauth.resource.keycloak.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum KeycloakClaimNames {
    ISS("iss"),
    SUB("sub"),
    AUD("aud"),
    EXP("exp"),
    NBF("nbf"),
    IAT("iat"),
    JTI("jti"),
    RESOURCE_ACCESS("resource_access"),
    ALLOWED_ORIGINS("allowed-origins"),
    TYP("typ"),
    PREFERRED_USERNAME("preferred_username"),
    SID("sid"),
    ACR("acr"),
    REALM_ACCESS("realm_access"),
    AZP("azp"),
    AUTH_TIME("auth_time"),
    SCOPE("scope"),
    SESSION_STATE("session_state"),
    EMAIL("email"),
    GIVEN_NAME("given_name"),
    FAMILY_NAME("family_name");

    private final String name;

    public static Set<String> names() {
        return Arrays.stream(values()).map(KeycloakClaimNames::getName).collect(Collectors.toSet());
    }
}