package net.trellisframework.oauth.resource.keycloak.action;

import jakarta.servlet.http.HttpServletRequest;
import net.trellisframework.context.action.Action1;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;

import java.util.Optional;

public interface FindTenantByIdAction extends Action1<Optional<OAuth2ResourceServerProperties.Jwt>, HttpServletRequest> {

}
