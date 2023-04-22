package net.trellisframework.message.action;

import net.trellisframework.message.constant.SendMessageStatus;
import net.trellisframework.message.payload.SendFireBaseNotificationRequest;
import com.google.common.base.Strings;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import net.trellisframework.context.action.Action2;
import net.trellisframework.core.log.Logger;
import net.trellisframework.message.payload.FireBaseConfiguration;
import net.trellisframework.message.payload.SendMessageResponse;
import net.trellisframework.message.task.BuildFireBaseMessageTask;
import net.trellisframework.message.task.InitializeFireBaseTask;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SendFireBaseNotificationWithConfigurationAction extends Action2<SendMessageResponse, FireBaseConfiguration, SendFireBaseNotificationRequest> {

    @Override
    public SendMessageResponse execute(FireBaseConfiguration configuration, SendFireBaseNotificationRequest request) {
        try {
            FirebaseApp app = call(InitializeFireBaseTask.class, configuration);
            Message message = call(BuildFireBaseMessageTask.class, request);
            String response = FirebaseMessaging.getInstance(app).send(message);
            return SendMessageResponse.of(Optional.ofNullable(response).map(Strings::emptyToNull).map(x -> SendMessageStatus.SUCCESS).orElse(SendMessageStatus.FAILED));
        } catch (Exception e) {
            Logger.error("SendFireBaseNotificationException", e.getMessage());
            return SendMessageResponse.error(e.getMessage());
        }
    }

}
