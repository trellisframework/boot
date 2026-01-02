package net.trellisframework.workflow.temporal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {
    
    /**
     * Temporal server address.
     * Example: localhost:7233
     */
    private String target = "localhost:7233";
    
    /**
     * Temporal namespace.
     * Default: ${spring.application.mode} or "default"
     */
    private String namespace;
    
    /**
     * Task queue name for this application/service.
     * Default: ${spring.application.name} or "default"
     */
    private String taskQueue;

}
