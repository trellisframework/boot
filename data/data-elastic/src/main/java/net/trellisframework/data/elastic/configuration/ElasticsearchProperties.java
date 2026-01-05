package net.trellisframework.data.elastic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.elasticsearch")
@Data
public class ElasticsearchProperties {
    private List<String> uris = new ArrayList<>();
    private String username;
    private String password;
    private Duration connectionTimeout;
}

