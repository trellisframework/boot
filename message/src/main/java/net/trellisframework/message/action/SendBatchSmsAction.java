package net.trellisframework.message.action;

import net.trellisframework.context.action.Action3;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.message.config.MessageConfiguration;
import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SendBatchSmsAction implements Action3<List<SendMessageResponse>, String , List<String>, String> {

    @Override
    public List<SendMessageResponse> execute(String config, List<String> recipients, String message) {
        MessageProperties.SmsPropertiesDefinition property = MessageConfiguration.getMessageProperty().getSms().get(config);
        if (Optional.ofNullable(property).map(MessageProperties.SmsPropertiesDefinition::getProvider).isEmpty())
            return null;
        return call(property.getProvider().getFactory(), property, recipients, message);
    }

}
