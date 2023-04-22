package net.trellisframework.websocket.config;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.log.Logger;
import net.trellisframework.websocket.payload.BrokerDefinition;
import net.trellisframework.websocket.payload.InterceptorDefinition;
import net.trellisframework.websocket.payload.WebSocketProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private static final String THREAD_NAME = "webSocketThreadPool-";

    public WebSocketConfig(WebSocketProperties properties, ApplicationContextProvider context) {
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(properties.getEndPoint()).setAllowedOriginPatterns(properties.getAllowedOriginPattern()).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(properties.getBrokers().stream().map(BrokerDefinition::getBroker).toArray(String[]::new));
        config.setApplicationDestinationPrefixes(properties.getApplicationDestinationPrefixes());
        config.setUserDestinationPrefix(properties.getUserDestinationPrefix());
        config.setCacheLimit(properties.getCacheLimit());
        config.setPreservePublishOrder(properties.isPreservePublishOrder());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(THREAD_NAME);
        executor.initialize();
        registration.taskExecutor(executor);
        ChannelInterceptor[] interceptors = createInterceptors(properties.getInterceptors());
        if (ObjectUtils.isNotEmpty(interceptors))
            registration.interceptors(interceptors);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(properties.getMaxTextMessageBufferSize());
        container.setMaxBinaryMessageBufferSize(properties.getMaxBinaryMessageBufferSize());
        return container;
    }

    private ChannelInterceptor[] createInterceptors(List<InterceptorDefinition> values) {
        if (values == null)
            return null;
        Set<ChannelInterceptor> interceptors = new HashSet<>();
        for (InterceptorDefinition interceptor : properties.getInterceptors()) {
            try {
                Class<?> clazz = Class.forName(interceptor.getName());
                interceptors.add((ChannelInterceptor) ApplicationContextProvider.context.getBean(clazz));
            } catch (Exception e) {
                Logger.error("CreateInterceptorException", "name: " + interceptor.getName() + " exception: " + e.getMessage());
            }
        }
        return interceptors.toArray(ChannelInterceptor[]::new);
    }

}
