package net.trellisframework.message.action;

import net.trellisframework.context.action.Action1;
import net.trellisframework.message.config.MailPropertiesDefinition;
import net.trellisframework.message.payload.SendMailRequest;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

@Service
public class SendMailAction implements Action1<SendMessageResponse, SendMailRequest> {

    @Override
    public SendMessageResponse execute(SendMailRequest request) {
        return call(SendMailWithConfigurationAction.class, MailPropertiesDefinition.getFromApplicationConfig(), request);
    }

}
