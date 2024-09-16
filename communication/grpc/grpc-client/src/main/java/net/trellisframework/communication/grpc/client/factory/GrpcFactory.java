package net.trellisframework.communication.grpc.client.factory;

import lombok.Data;
import org.springframework.cloud.client.ServiceInstance;

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

    public static GrpcFactory getInstance(String host, Integer port, Class<?> service) {
        return new GrpcFactory(host, port, 60, service);
    }

    public static GrpcFactory getInstance(String host, Class<?> service) {
        return new GrpcFactory(host, 6565, 60, service);
    }

    public static GrpcFactory getInstance(ServiceInstance instance , Class<?> service) {
        return getInstance(instance, 60, service);
    }

    public static GrpcFactory getInstance(ServiceInstance instance, Integer timeout , Class<?> service) {
        return new GrpcFactory(instance.getHost(), Integer.parseInt(instance.getMetadata().getOrDefault("grpc-port", "6565")), timeout, service);
    }

    public <T> T create(Class<T> serviceClass) {
        InvocationHandler handler = new GrpcInvocationHandler(host, port, timeout, service);
        return (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[]{serviceClass}, handler);
    }
}