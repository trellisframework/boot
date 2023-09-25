package net.trellisframework.util.config;

import net.trellisframework.util.aws.AwsS3ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AwsS3ClientProperties.class})
public class UtilConfig {

}
