package net.trellisframework.stream.kafka.producer;

import net.trellisframework.stream.core.payload.ProducerMessage;
import net.trellisframework.stream.core.prodcer.MessageProducer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageProducer implements MessageProducer {
    private final KafkaProducer<String, Object> template;

    public KafkaMessageProducer(KafkaProducer<String, Object> producer) {
        this.template = producer;
    }

    @Override
    public void send(ProducerMessage<?> message) {
        template.send(new ProducerRecord<>(message.getTopic(), message.getPartition(), message.getTimestamp(), message.getKey(), message.getPayload()));
        template.flush();
    }
}


