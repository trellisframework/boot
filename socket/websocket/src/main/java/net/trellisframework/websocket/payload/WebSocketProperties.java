package net.trellisframework.websocket.payload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Primary
@Configuration
@ConfigurationProperties(prefix = "socket.websocket")
@Data
public class WebSocketProperties {
    private List<BrokerDefinition> brokers = new ArrayList<>();
    private List<InterceptorDefinition> interceptors = new ArrayList<>();
    private String userDestinationPrefix;
    private int cacheLimit = 1024;
    private String endPoint = "/";
    private String allowedOriginPattern = "*";
    private String applicationDestinationPrefixes = "/";
    private boolean preservePublishOrder = true;
    private int corePoolSize = 7;
    private int maxPoolSize = 14;
    private int queueCapacity = 10;
    private int maxTextMessageBufferSize = 8192;
    private int maxBinaryMessageBufferSize = 8192;
}
