package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.constant.FireBasePlatform;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class SendFireBaseNotificationRequest implements Payload {
    private FireBasePlatform platform = FireBasePlatform.ALL;

    private String title;

    private String body;

    private String topic;

    private String token;

    private String imageUrl;

    private Map<String, String> data;
}
