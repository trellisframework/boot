package net.trellisframework.mcp.autoconfigure;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.Map;

@Endpoint(id = "mcp-sessions")
public class McpSessionEndpoint {
    private final McpSessionCleanupScheduler scheduler;

    public McpSessionEndpoint(McpSessionCleanupScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @ReadOperation
    Map<String, Object> status() {
        return scheduler.status();
    }

    @WriteOperation
    Map<String, Object> evict() {
        scheduler.cleanup();
        return scheduler.status();
    }
}
