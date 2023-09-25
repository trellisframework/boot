package net.trellisframework.communication.grpc.client.factory;

import lombok.Data;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.ServiceUnavailableException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@Data
public class GrpcFactory {
    private String host;
    private Integer port;
    private Integer timeout;
    private Class<?> service;

    private GrpcFactory(String host, Integer port, Integer timeout, Class<?> service) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.service = service;
    }

    public static GrpcFactory getInstance(String host, Integer port, Integer timeout, Class<?> service) {
        return new GrpcFactory(host, port, timeout, service);
    }

    public static GrpcFactory getInstance(String serviceId, Class<?> service) {
        return getInstance(serviceId, 60, service);
    }

    public static GrpcFactory getInstance(String serviceId, Integer timeout, Class<?> service) {
        ServiceInstance instance = ApplicationContextProvider.context.getBean(LoadBalancerClient.class).choose(serviceId);
        if (instance == null)
            throw new ServiceUnavailableException(Messages.SERVICE_UNAVAILABLE);
        return new GrpcFactory(instance.getHost(), Integer.parseInt(instance.getMetadata().getOrDefault("grpc-port", "6565")), timeout, service);
    }

    public <T> T create(Class<T> serviceClass) {
        InvocationHandler handler = new GrpcInvocationHandler(host, port, timeout, service);
        return (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[]{serviceClass}, handler);
    }
}