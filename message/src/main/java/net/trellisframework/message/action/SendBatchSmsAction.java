package net.trellisframework.message.action;

import net.trellisframework.context.action.Action3;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.config.SmsPropertiesDefinition;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SendBatchSmsAction implements Action3<List<SendMessageResponse>, String , List<String>, String> {

    @Override
    public List<SendMessageResponse> execute(String config, List<String> recipients, String message) {
        MessageProperties properties = ApplicationContextProvider.context.getBean(MessageProperties.class);
        SmsPropertiesDefinition property = properties.getSms().get(config);
        if (Optional.ofNullable(property).map(SmsPropertiesDefinition::getProvider).isEmpty())
            return null;
        return call(property.getProvider().getFactory(), property, recipients, message);
    }

}
