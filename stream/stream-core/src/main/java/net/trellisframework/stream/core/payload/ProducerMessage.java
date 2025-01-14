package net.trellisframework.stream.core.payload;

import lombok.*;
import org.springframework.messaging.MessageHeaders;

@Builder
@Data
@AllArgsConstructor(staticName = "of")
public class ProducerMessage<T> implements org.springframework.messaging.Message<T> {
    private String exchange;
    private String topic;
    private String key;
    private Integer partition;
    private Long timestamp;
    private T payload;
    private MessageHeaders headers;
}
