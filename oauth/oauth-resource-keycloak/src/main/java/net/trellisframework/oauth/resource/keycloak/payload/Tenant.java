package net.trellisframework.oauth.resource.keycloak.payload;

public interface Tenant {

    String getIssuer();

    String getJwkSetUrl();

}