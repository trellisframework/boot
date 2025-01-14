package net.trellisframework.stream.kafka.config;

import net.trellisframework.stream.kafka.serde.JdkSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.cloud.stream.kafka.binder")
    KafkaBinderConfigurationProperties configurationProperties(KafkaProperties kafkaProperties, ObjectProvider<KafkaConnectionDetails> kafkaConnectionDetails) {
        return new KafkaBinderConfigurationProperties(kafkaProperties, kafkaConnectionDetails);
    }

    @Bean
    public KafkaProducer<String, Object> kafkaProducer(KafkaBinderConfigurationProperties properties) {
        Properties props = new Properties();
        props.put("bootstrap.servers", List.of(properties.getBrokers()));
        props.put("key.serializer", properties.getProducerProperties().getOrDefault("key.serializer", StringSerializer.class.getName()));
        props.put("value.serializer", properties.getProducerProperties().getOrDefault("value.serializer", JdkSerializer.class.getName()));
        return new KafkaProducer<>(props);
    }
}
