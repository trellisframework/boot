package net.trellisframework.message.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MessageProperties.class})
public class MessageConfiguration {

}