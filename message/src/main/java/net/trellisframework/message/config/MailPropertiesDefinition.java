package net.trellisframework.message.config;

import net.trellisframework.util.environment.EnvironmentUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Payload;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class MailPropertiesDefinition implements Payload {
    private String host;

    private String port;

    private String username;

    private String password;

    private String from;

    private String enableAuthentication;

    private String enableStartTLS;

    private String enableSSL;

    public static MailPropertiesDefinition getFromApplicationConfig() {
        String host = EnvironmentUtil.getPropertyValue("spring.mail.host", StringUtils.EMPTY);
        String port = EnvironmentUtil.getPropertyValue("spring.mail.port", StringUtils.EMPTY);
        String username = EnvironmentUtil.getPropertyValue("spring.mail.username", StringUtils.EMPTY);
        String password = EnvironmentUtil.getPropertyValue("spring.mail.password", StringUtils.EMPTY);
        String from = EnvironmentUtil.getPropertyValue("spring.mail.properties.from", StringUtils.EMPTY);
        String enableAuthentication = EnvironmentUtil.getPropertyValue("spring.mail.properties.enable-authentication", "false");
        String enableStartTLS = EnvironmentUtil.getPropertyValue("spring.mail.properties.enable-start-tls", "false");
        String enableSSL = EnvironmentUtil.getPropertyValue("spring.mail.properties.enable-ssl", "false");
        return new MailPropertiesDefinition(host, port, username, password, from, enableAuthentication, enableStartTLS, enableSSL);
    }
}
