package net.trellisframework.communication.grpc.client.annotation;

import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GrpcMethod {
    String value() default "";

    Class<? extends HttpErrorMessage> exception() default HttpErrorMessage.class;
}
