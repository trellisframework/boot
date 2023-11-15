package net.trellisframework.communication.grpc.client.factory;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;
import net.trellisframework.communication.grpc.client.annotation.GrpcMethod;
import net.trellisframework.communication.grpc.client.constant.Messages;
import net.trellisframework.communication.grpc.client.payload.MethodInfo;
import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.http.exception.ProcessingException;
import net.trellisframework.util.json.JsonUtil;
import net.trellisframework.util.string.StringUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcInvocationHandler implements InvocationHandler {
    private final ManagedChannel channel;
    private final int timeout;
    private final Class<?> service;

    private final static Map<Method, MethodInfo> endpointsCache = new ConcurrentHashMap<>();

    public GrpcInvocationHandler(String host, Integer port, Integer timeout, Class<?> service) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        this.timeout = timeout;
        this.service = service;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        MethodInfo endpoint = loadEndpoint(method);
        if (endpoint != null) {
            AbstractBlockingStub<?> stub = createGrpcStub(service, channel);
            stub = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS);
            Object request = args[0];
            return invokeGrpcMethod(stub, endpoint.getName(), request, endpoint.getException());
        }
        throw new UnsupportedOperationException("Method not supported: " + method.getName());
    }

    private AbstractBlockingStub<?> createGrpcStub(Class<?> serviceClass, Channel channel) {
        try {
            Method newBlockingStubMethod = serviceClass.getDeclaredMethod("newBlockingStub", Channel.class);
            return (AbstractBlockingStub<?>) newBlockingStubMethod.invoke(null, channel);
        } catch (Exception e) {
            throw new ProcessingException(Messages.FAILED_TO_CREATE_GRPC_STUB);
        }
    }

    private Object invokeGrpcMethod(AbstractBlockingStub<?> stub, String methodName, Object request, Class<? extends HttpErrorMessage> clazz) {
        try {
            Method method = stub.getClass().getMethod(methodName, request.getClass());
            return method.invoke(stub, request);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException p && p.getTargetException() instanceof StatusRuntimeException ex) {
                String message = Optional.ofNullable(ex.getStatus().getDescription()).orElse(e.getMessage());
                throw new HttpException(JsonUtil.toObject(message, clazz));
            }
            throw new ProcessingException(Messages.FAILED_TO_INVOKE_GRPC_METHOD);
        } finally {
            channel.shutdown();
        }
    }

    private MethodInfo loadEndpoint(Method method) {
        MethodInfo result = endpointsCache.get(method);
        if (result != null) return result;

        synchronized (endpointsCache) {
            result = endpointsCache.get(method);
            if (result == null && method.isAnnotationPresent(GrpcMethod.class)) {
                GrpcMethod endpoint = method.getAnnotation(GrpcMethod.class);
                result = MethodInfo.of(Optional.ofNullable(endpoint.value()).map(StringUtil::nullIfBlank).orElse(method.getName()), endpoint.exception());
                endpointsCache.put(method, result);
            }
        }
        return result;
    }
}
