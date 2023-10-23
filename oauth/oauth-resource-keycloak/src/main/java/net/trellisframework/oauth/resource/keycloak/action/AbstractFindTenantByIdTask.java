package net.trellisframework.oauth.resource.keycloak.action;

import net.trellisframework.context.action.Action1;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;

import java.util.Optional;

public abstract class AbstractFindTenantByIdTask implements Action1<Optional<OAuth2ResourceServerProperties.Jwt>, String> {

}
