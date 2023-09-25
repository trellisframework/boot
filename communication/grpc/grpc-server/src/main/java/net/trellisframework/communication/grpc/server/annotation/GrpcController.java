package net.trellisframework.communication.grpc.server.annotation;

import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GrpcController {
    Class<? extends ServerInterceptor>[] interceptors() default {};
    boolean applyGlobalInterceptors() default true;
}
