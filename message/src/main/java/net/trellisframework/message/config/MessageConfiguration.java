package net.trellisframework.message.config;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MailProperties.class})
public class MessageConfiguration {
    private static MailProperties mail;

    public static MailProperties getMailProperty() {
        if (mail == null)
            mail = ApplicationContextProvider.context.getBean(MailProperties.class);
        return mail;
    }

}