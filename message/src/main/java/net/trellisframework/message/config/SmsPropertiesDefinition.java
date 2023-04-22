package net.trellisframework.message.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.trellisframework.message.constant.SmsProvider;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.Payload;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Validated
public class SmsPropertiesDefinition implements Payload {
    private SmsProvider provider;

    private String username;

    private String password;

    private String from;

    private String domain;
}
