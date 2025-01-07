package net.trellisframework.stream.kafka.producer;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

public class StreamBridge {

    static KafkaTemplate<String, String> template;

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, String> getConnection() {
        if (template == null)
            template = ApplicationContextProvider.context.getBean(KafkaTemplate.class);
        return template;
    }

    public static void send(String topic, Object value) {
        getConnection().send(topic, Optional.ofNullable(value).map(Object::toString).orElse(null));
        getConnection().flush();
    }

    public static void send(String topic, Object key, Object value) {
        getConnection().send(topic, Optional.ofNullable(key).map(Object::toString).orElse(StringUtils.EMPTY), Optional.ofNullable(value).map(Object::toString).orElse(null));
        getConnection().flush();
    }


    public static void send(String topic, Integer partition, Object key, Object data) {
        getConnection().send(new ProducerRecord<>(topic, partition, Optional.ofNullable(key).map(Object::toString).orElse(StringUtils.EMPTY), Optional.ofNullable(data).map(Object::toString).orElse(null)));
        getConnection().flush();
    }

    public void send(String topic, Integer partition, Long timestamp, Object key, Object data) {
        getConnection().send(new ProducerRecord<>(topic, partition, timestamp, Optional.ofNullable(key).map(Object::toString).orElse(StringUtils.EMPTY), Optional.ofNullable(data).map(Object::toString).orElse(null)));
        getConnection().flush();
    }
}