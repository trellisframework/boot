package net.trellisframework.message.config;

import lombok.Data;

import javax.validation.Payload;

@Data
public class FcmPropertiesDefinition implements Payload {
    private String credential;

    private String name;
}
