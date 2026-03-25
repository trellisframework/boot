package net.trellisframework.mcp.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "spring.ai.mcp.server.session")
@Data
public class McpSessionCleanupProperties {
    private Duration maxAge = Duration.ofMinutes(30);
}
