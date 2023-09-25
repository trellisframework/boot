package net.trellisframework.message.action;

import net.trellisframework.message.payload.SendFireBaseNotificationRequest;
import net.trellisframework.context.action.Action1;
import net.trellisframework.core.log.Logger;
import net.trellisframework.message.payload.FireBaseConfiguration;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

@Service
public class SendFireBaseNotificationAction implements Action1<SendMessageResponse, SendFireBaseNotificationRequest> {

    @Override
    public SendMessageResponse execute(SendFireBaseNotificationRequest request) {
        try {
            return call(SendFireBaseNotificationWithConfigurationAction.class, FireBaseConfiguration.getFromApplicationConfig(), request);
        } catch (Exception e) {
            Logger.error("SendFireBaseNotificationException", e.getMessage());
            return SendMessageResponse.ok();
        }
    }

}
