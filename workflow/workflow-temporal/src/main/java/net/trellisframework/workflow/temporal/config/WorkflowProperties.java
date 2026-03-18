package net.trellisframework.workflow.temporal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {
    private String target = "localhost:7233";
    private String namespace;
    private String taskQueue;
    private int retentionDays = 7;
    private int maxConcurrentWorkflowTasks = 200;
    private int maxConcurrentActivities = 200;
    private long deadlockDetectionTimeout = 60000;

}
