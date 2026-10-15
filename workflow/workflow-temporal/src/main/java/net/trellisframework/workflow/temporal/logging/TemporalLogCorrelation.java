package net.trellisframework.workflow.temporal.logging;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public final class TemporalLogCorrelation implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY = "logging.pattern.correlation";
    public static final String PATTERN = "%notEmpty{[%X{WorkflowType} %X{WorkflowId}] }%notEmpty{[%X{ActivityType}#%X{Attempt}] }";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configured = environment.getProperty(PROPERTY, "");
        if (!configured.contains("WorkflowId"))
            environment.getPropertySources().addFirst(new MapPropertySource("temporalLogCorrelation", Map.of(PROPERTY, configured + PATTERN)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
