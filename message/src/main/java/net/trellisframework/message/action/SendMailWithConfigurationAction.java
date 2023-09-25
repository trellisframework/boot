package net.trellisframework.message.action;

import net.trellisframework.context.action.Action2;
import net.trellisframework.core.log.Logger;
import net.trellisframework.message.config.MailPropertiesDefinition;
import net.trellisframework.message.payload.EmbeddedData;
import net.trellisframework.message.payload.SendMailRequest;
import net.trellisframework.message.payload.SendMessageResponse;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class SendMailWithConfigurationAction implements Action2<SendMessageResponse, MailPropertiesDefinition, SendMailRequest> {

    @Override
    public SendMessageResponse execute(MailPropertiesDefinition config, SendMailRequest request) {
        try {
            EmailPopulatingBuilder builder = EmailBuilder.startingBlank().from(config.getFrom()).withRecipient(request.getEmail(), request.getEmail(), Message.RecipientType.TO).withSubject(request.getSubject()).withPlainText("").withHTMLText(request.getBody());
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getHost());
            props.put("mail.smtp.port", config.getPort());
            props.put("mail.smtp.auth", config.getEnableAuthentication());
            props.put("mail.smtp.starttls.enable", config.getEnableStartTLS());
            props.put("mail.smtp.ssl.enable", config.getEnableSSL());
            if (request.getPdf() != null) {
                builder.withAttachment(request.getPdfName(), request.getPdf(), MediaType.APPLICATION_PDF_VALUE);
            }
            if (request.getImage() != null) {
                builder.withAttachment(request.getImageName(), request.getImage(), MediaType.IMAGE_PNG_VALUE);
            }
            if (request.getEmbeddedData() != null && !request.getEmbeddedData().isEmpty()) {
                for (EmbeddedData embeddedData : request.getEmbeddedData()) {
                    if (embeddedData != null && embeddedData.getData() != null) {
                        builder.withAttachment(embeddedData.getName(), embeddedData.getData(), embeddedData.getMimeType());
                    }
                }
            }
            Email email = new Email(builder);
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });
            MimeMessage mimeMessage = EmailConverter.emailToMimeMessage(email, session);
            Transport.send(mimeMessage);
            return SendMessageResponse.ok();
        } catch (Exception e) {
            Logger.error("signMessage", e.getMessage());
            return SendMessageResponse.error(e.getMessage());
        }
    }
}
