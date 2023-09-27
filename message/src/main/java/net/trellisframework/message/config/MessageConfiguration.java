package net.trellisframework.message.config;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.NotFoundException;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties({MessageProperties.class, MailProperties.class})
public class MessageConfiguration {

    private static MessageProperties message;
    private static MailProperties mail;



    public static MessageProperties getMessageProperty() {
        if (message == null)
            message = ApplicationContextProvider.context.getBean(MessageProperties.class);
        return message;
    }

    public static MailProperties getMailProperty() {
        if (mail == null)
            mail = ApplicationContextProvider.context.getBean(MailProperties.class);
        return mail;
    }

}