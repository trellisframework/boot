package net.trellisframework.stream.rabbit.producer;

import net.trellisframework.stream.core.payload.ProducerMessage;
import net.trellisframework.stream.core.prodcer.MessageProducer;
import net.trellisframework.util.json.JsonUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMessageProducer implements MessageProducer {
    private final RabbitTemplate template;

    public RabbitMessageProducer(RabbitTemplate template) {
        this.template = template;
    }

    @Override
    public void send(ProducerMessage<?> message) {
        template.send(message.getExchange(), message.getTopic(), MessageBuilder
                .withBody(ObjectUtils.isEmpty(message.getPayload()) ? null : JsonUtil.toString(message.getPayload()).getBytes())
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build());
    }
}
