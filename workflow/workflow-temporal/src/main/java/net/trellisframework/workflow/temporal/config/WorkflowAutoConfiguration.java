package net.trellisframework.workflow.temporal.config;

import io.temporal.api.enums.v1.IndexedValueType;
import io.temporal.api.operatorservice.v1.AddSearchAttributesRequest;
import io.temporal.api.operatorservice.v1.ListSearchAttributesRequest;
import io.temporal.api.operatorservice.v1.OperatorServiceGrpc;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.VersioningBehavior;
import io.temporal.common.WorkerDeploymentVersion;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerDeploymentOptions;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import net.trellisframework.core.log.Logger;
import net.trellisframework.util.thread.Threads;
import net.trellisframework.workflow.temporal.activity.DynamicTaskActivity;
import net.trellisframework.workflow.temporal.annotation.Workflow;
import net.trellisframework.workflow.temporal.workflow.DynamicWorkflowAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(WorkflowProperties.class)
public class WorkflowAutoConfiguration {

    private static final String CONCURRENCY_KEY = DynamicWorkflowAction.SEARCH_ATTR_CONCURRENCY_KEY;
    private static final int NAMESPACE_RETENTION_DAYS = 7;

    static {
        Configurator.setLevel("io.temporal.internal.activity", Level.OFF);
    }

    @Value("${spring.application.name:default}")
    private String applicationName;

    @Value("${spring.application.mode:default}")
    private String applicationMode;

    @Bean
    @ConditionalOnMissingBean
    public WorkflowServiceStubs workflowServiceStubs(WorkflowProperties properties) {
        WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(properties.getTarget()).build());
        String namespace = getNamespace(properties);
        ensureNamespaceExists(stubs, namespace);
        ensureSearchAttributeExists(stubs, namespace);
        return stubs;
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs, WorkflowProperties properties) {
        return WorkflowClient.newInstance(stubs,
                WorkflowClientOptions.newBuilder().setNamespace(getNamespace(properties)).build());
    }

    @Bean
    @ConditionalOnMissingBean
    public WorkerFactory workerFactory(WorkflowClient client) {
        return WorkerFactory.newInstance(client);
    }

    @Bean
    public WorkerInitializer workerInitializer(WorkerFactory factory, WorkflowProperties properties, ApplicationContext ctx) {
        String taskQueue = StringUtils.defaultIfBlank(properties.getTaskQueue(), applicationName);
        return new WorkerInitializer(factory, taskQueue, ctx);
    }

    private String getNamespace(WorkflowProperties properties) {
        return StringUtils.defaultIfBlank(properties.getNamespace(), applicationMode);
    }

    private void ensureNamespaceExists(WorkflowServiceStubs stubs, String namespace) {
        try {
            var stub = WorkflowServiceGrpc.newBlockingStub(stubs.getRawChannel());
            stub.registerNamespace(RegisterNamespaceRequest.newBuilder()
                    .setNamespace(namespace)
                    .setWorkflowExecutionRetentionPeriod(
                            com.google.protobuf.Duration.newBuilder()
                                    .setSeconds(NAMESPACE_RETENTION_DAYS * 24 * 60 * 60).build())
                    .build());
            Logger.info("Temporal", "Created namespace: %s", namespace);
            Threads.sleep(2000);
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() != io.grpc.Status.Code.ALREADY_EXISTS) {
                Logger.warn("Temporal", "Could not create namespace: %s", e.getMessage());
            }
        }
    }

    private void ensureSearchAttributeExists(WorkflowServiceStubs stubs, String namespace) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                var stub = OperatorServiceGrpc.newBlockingStub(stubs.getRawChannel());
                var existing = stub.listSearchAttributes(
                        ListSearchAttributesRequest.newBuilder().setNamespace(namespace).build())
                        .getCustomAttributesMap();

                if (!existing.containsKey(CONCURRENCY_KEY)) {
                    stub.addSearchAttributes(AddSearchAttributesRequest.newBuilder()
                            .setNamespace(namespace)
                            .putSearchAttributes(CONCURRENCY_KEY, IndexedValueType.INDEXED_VALUE_TYPE_KEYWORD)
                            .build());
                    Logger.info("Temporal", "Registered search attribute: %s", CONCURRENCY_KEY);
                    Threads.sleep(2000);
                }
                return;
            } catch (Exception e) {
                Logger.warn("Temporal", "Attempt %d/5 - Could not register search attributes: %s", attempt, e.getMessage());
                if (attempt < 5) Threads.sleep(1000 * attempt);
            }
        }
    }

    public static class WorkerInitializer {

        public WorkerInitializer(WorkerFactory factory, String taskQueue, ApplicationContext ctx) {
            Set<String> versions = collectVersions(ctx);

            if (versions.isEmpty()) {
                createWorker(factory, taskQueue, null);
            } else {
                versions.forEach(v -> createWorker(factory, taskQueue, v));
            }
            factory.start();
        }

        private Set<String> collectVersions(ApplicationContext ctx) {
            Set<String> versions = new HashSet<>();
            Map<String, Object> beans = ctx.getBeansWithAnnotation(Workflow.class);
            for (Object bean : beans.values()) {
                Workflow annotation = bean.getClass().getAnnotation(Workflow.class);
                if (annotation != null && !annotation.version().isEmpty()
                        && !Workflow.DEFAULT_VERSION.equals(annotation.version())) {
                    versions.add(annotation.version());
                }
            }
            return versions;
        }

        private void createWorker(WorkerFactory factory, String taskQueue, String version) {
            WorkerOptions.Builder options = WorkerOptions.newBuilder();
            if (version != null) {
                options.setDeploymentOptions(WorkerDeploymentOptions.newBuilder()
                        .setVersion(new WorkerDeploymentVersion(taskQueue, version))
                        .setUseVersioning(true)
                        .setDefaultVersioningBehavior(VersioningBehavior.AUTO_UPGRADE)
                        .build());
            }
            Worker worker = factory.newWorker(taskQueue, options.build());
            worker.registerWorkflowImplementationTypes(DynamicWorkflowAction.class);
            worker.registerActivitiesImplementations(new DynamicTaskActivity());
            Logger.info("Temporal", "Worker started on queue: %s%s", taskQueue,
                    version != null ? ", version: " + version : "");
        }
    }
}
