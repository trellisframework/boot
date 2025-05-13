package net.trellisframework.util.aws;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import net.trellisframework.core.payload.Payload;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("aws.s3")
@Data
public class AwsS3ClientProperties {

    private final Map<String, S3PropertiesDefinition> buckets = new HashMap<>();

    @Data
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Validated
    public static class S3PropertiesDefinition {
        private String endpoint;
        private Regions region;
        private Credential credential;
        private Boolean pathStyle;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @Validated
    public static class Credential implements Payload {
        private String accessKey;
        private String secretKey;
    }

}