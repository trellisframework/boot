package net.trellisframework.mcp.autoconfigure;

import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration(after = McpServerStreamableHttpWebMvcAutoConfiguration.class)
@ConditionalOnClass(WebMvcStreamableServerTransportProvider.class)
@ConditionalOnBean(WebMvcStreamableServerTransportProvider.class)
@EnableConfigurationProperties(McpSessionCleanupProperties.class)
public class McpSessionCleanupAutoConfiguration {

    @Bean
    McpSessionCleanupScheduler mcpSessionCleanupScheduler(WebMvcStreamableServerTransportProvider transportProvider, McpSessionCleanupProperties properties) {
        return new McpSessionCleanupScheduler(transportProvider, properties.getMaxAge(), properties.getIdleTimeout(), properties.getCleanupInterval());
    }

    @Bean
    McpSessionEndpoint mcpSessionEndpoint(McpSessionCleanupScheduler scheduler) {
        return new McpSessionEndpoint(scheduler);
    }

    @Bean
    FilterRegistrationBean<McpSessionActivityFilter> mcpSessionActivityFilter(McpSessionCleanupScheduler scheduler, McpSessionCleanupProperties properties) {
        FilterRegistrationBean<McpSessionActivityFilter> registration = new FilterRegistrationBean<>(new McpSessionActivityFilter(scheduler, properties.isInterceptUnauthorized()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("mcpSessionActivityFilter");
        return registration;
    }
}
