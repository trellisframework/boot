package net.trellisframework.message.task;

import net.trellisframework.message.constant.FireBaseNotificationParameter;
import net.trellisframework.message.constant.FireBasePlatform;
import net.trellisframework.message.payload.SendFireBaseNotificationRequest;
import com.google.firebase.messaging.*;
import net.trellisframework.context.task.Task1;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class BuildFireBaseMessageTask extends Task1<Message, SendFireBaseNotificationRequest> {

    @Override
    public Message execute(SendFireBaseNotificationRequest request) {
        Message.Builder messageBuilder = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .setImage(request.getImageUrl())
                        .build());
        if (request.getPlatform() != null && request.getPlatform().isIn(FireBasePlatform.ALL, FireBasePlatform.WEB))
            messageBuilder.setWebpushConfig(WebpushConfig.builder().setNotification(WebpushNotification.builder()
                    .setTitle(request.getTitle())
                    .setBody(request.getBody())
                    .setImage(request.getImageUrl())
                    .build())
                    .build());
        if (request.getPlatform() != null && request.getPlatform().isIn(FireBasePlatform.ALL, FireBasePlatform.ANDROID))
            messageBuilder.setAndroidConfig(AndroidConfig.builder()
                    .setTtl(Duration.ofMinutes(2).toMillis())
                    .setCollapseKey(request.getTopic())
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification
                            .builder()
                            .setSound(FireBaseNotificationParameter.SOUND.getValue())
                            .setColor(FireBaseNotificationParameter.COLOR.getValue())
                            .setTag(request.getTopic())
                            .build())
                    .build());
        if (request.getPlatform() != null && request.getPlatform().isIn(FireBasePlatform.ALL, FireBasePlatform.IOS))
            messageBuilder.setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setCategory(request.getTopic())
                    .setThreadId(request.getTopic())
                    .build()
            ).build());
        if (StringUtils.isNoneEmpty(request.getTopic()))
            messageBuilder.setTopic(request.getTopic());
        else if (StringUtils.isNoneEmpty(request.getToken()))
            messageBuilder.setToken(request.getToken());
        if (request.getData() != null && !request.getData().isEmpty())
            messageBuilder.putAllData(request.getData());
        return messageBuilder.build();
    }

}
