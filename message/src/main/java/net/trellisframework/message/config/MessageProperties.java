package net.trellisframework.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("trellis.message")
@Validated
@Data
public class MessageProperties {

    private Map<String, SmsPropertiesDefinition> sms = new HashMap<>();

    private Map<String, FcmPropertiesDefinition> fcm = new HashMap<>();

}