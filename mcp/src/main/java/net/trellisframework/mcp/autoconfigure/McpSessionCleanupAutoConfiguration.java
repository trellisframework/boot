package net.trellisframework.mcp.autoconfigure;

import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = McpServerStreamableHttpWebMvcAutoConfiguration.class)
@ConditionalOnClass(WebMvcStreamableServerTransportProvider.class)
@EnableConfigurationProperties(McpSessionCleanupProperties.class)
public class McpSessionCleanupAutoConfiguration {

    @Bean
    McpSessionCleanupScheduler mcpSessionCleanupScheduler(WebMvcStreamableServerTransportProvider transportProvider, McpSessionCleanupProperties properties) {
        return new McpSessionCleanupScheduler(transportProvider, properties.getMaxAge());
    }

    @Bean
    McpSessionEndpoint mcpSessionEndpoint(McpSessionCleanupScheduler scheduler) {
        return new McpSessionEndpoint(scheduler);
    }

}
