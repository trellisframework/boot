package net.trellisframework.websocket.payload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Primary
@Configuration
@ConfigurationProperties(prefix = "socket.websocket")
public class WebSocketProperties {

    private List<BrokerDefinition> brokers = new ArrayList<>();
    private List<InterceptorDefinition> interceptors = new ArrayList<>();
    private String userDestinationPrefix;
    private int cacheLimit = 1024;
    private String endPoint = "/";
    private String allowedOriginPattern = "*";
    private String applicationDestinationPrefixes = "/";
    private boolean preservePublishOrder = true;
    private int CorePoolSize = 7;
    private int MaxPoolSize = 14;
    private int QueueCapacity = 10;
    private int maxTextMessageBufferSize = 8192;
    private int maxBinaryMessageBufferSize = 8192;

    public List<BrokerDefinition> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<BrokerDefinition> brokers) {
        this.brokers = brokers;
    }

    public List<InterceptorDefinition> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<InterceptorDefinition> interceptors) {
        this.interceptors = interceptors;
    }

    public String getUserDestinationPrefix() {
        return userDestinationPrefix;
    }

    public void setUserDestinationPrefix(String userDestinationPrefix) {
        this.userDestinationPrefix = userDestinationPrefix;
    }

    public int getCacheLimit() {
        return cacheLimit;
    }

    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getAllowedOriginPattern() {
        return allowedOriginPattern;
    }

    public void setAllowedOriginPattern(String allowedOriginPattern) {
        this.allowedOriginPattern = allowedOriginPattern;
    }

    public String getApplicationDestinationPrefixes() {
        return applicationDestinationPrefixes;
    }

    public void setApplicationDestinationPrefixes(String applicationDestinationPrefixes) {
        this.applicationDestinationPrefixes = applicationDestinationPrefixes;
    }

    public boolean isPreservePublishOrder() {
        return preservePublishOrder;
    }

    public void setPreservePublishOrder(boolean preservePublishOrder) {
        this.preservePublishOrder = preservePublishOrder;
    }

    public int getCorePoolSize() {
        return CorePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        CorePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return MaxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        MaxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return QueueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        QueueCapacity = queueCapacity;
    }

    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }

    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    public void setMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

}
