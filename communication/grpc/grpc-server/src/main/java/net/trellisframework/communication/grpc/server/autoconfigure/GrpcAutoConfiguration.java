package net.trellisframework.communication.grpc.server.autoconfigure;

import net.trellisframework.communication.grpc.server.annotation.GrpcController;
import net.trellisframework.communication.grpc.server.annotation.OnGrpcServerEnabled;
import net.trellisframework.communication.grpc.server.properties.GrpcServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfigureOrder
@AutoConfigureAfter(ValidationAutoConfiguration.class)
@ConditionalOnBean(annotation = GrpcController.class)
@Import({GrpcServerProperties.class})
@Configuration
public class GrpcAutoConfiguration {

    @Bean
    @OnGrpcServerEnabled
    public GrpcServerRunner grpcServerRunner(GrpcServerProperties properties) {
        return new GrpcServerRunner(properties);
    }

    @Bean
    public GrpcServicesRegistry grpcServicesRegistry() {
        return new GrpcServicesRegistry();
    }
}