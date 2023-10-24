package net.trellisframework.oauth.resource.keycloak.task;

import net.trellisframework.context.task.Task1;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;

import java.util.Optional;

public interface FindTenantByIdAction extends Task1<Optional<OAuth2ResourceServerProperties.Jwt>, String> {

}
