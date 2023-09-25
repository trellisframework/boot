package net.trellisframework.communication.grpc.server.autoconfigure;

import io.grpc.BindableService;
import lombok.Builder;
import lombok.Getter;
import net.trellisframework.communication.grpc.server.annotation.GrpcController;
import net.trellisframework.core.application.ApplicationContextProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.function.SingletonSupplier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GrpcServicesRegistry implements InitializingBean {

    @Getter
    @Builder
    public static class GrpcServiceMethod {
        private BindableService service;
        private Method method;

    }

    private Supplier<Map<String, BindableService>> beanNameToServiceBean;

    public Map<String, BindableService> getBeanNameToServiceBeanMap() {
        return beanNameToServiceBean.get();
    }

    private <T> Map<String, T> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) {
        return ApplicationContextProvider.context.getBeansWithAnnotation(annotationType)
                .entrySet()
                .stream()
                .filter(e -> beanType.isInstance(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> beanType.cast(e.getValue())));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        beanNameToServiceBean = SingletonSupplier.of(() -> getBeanNamesByTypeWithAnnotation(GrpcController.class, BindableService.class));
    }
}