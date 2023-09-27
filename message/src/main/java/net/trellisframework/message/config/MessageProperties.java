package net.trellisframework.message.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import net.trellisframework.message.constant.SmsProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Payload;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("trellis.message")
@Validated
@Data
public class MessageProperties {

    private Map<String, SmsPropertiesDefinition> sms = new HashMap<>();

    private Map<String, FcmPropertiesDefinition> fcm = new HashMap<>();


    @Data
    public static class FcmPropertiesDefinition implements Payload {
        private String credential;

        private String name;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Validated
    public static class SmsPropertiesDefinition implements Payload {
        private SmsProvider provider;

        private String username;

        private String password;

        private String from;

        private String domain;
    }


}