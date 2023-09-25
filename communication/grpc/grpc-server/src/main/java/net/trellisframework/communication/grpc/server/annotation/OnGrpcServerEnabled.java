package net.trellisframework.communication.grpc.server.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty(value = "grpc.enabled", havingValue = "true", matchIfMissing = true)
public @interface OnGrpcServerEnabled {
}
