package net.trellisframework.communication.grpc.server.provider;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.trellisframework.context.action.*;
import net.trellisframework.context.provider.InjectorBeanProvider;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.http.exception.InternalServerException;

public interface GrpcActionContextProvider extends InjectorBeanProvider {

    default <A extends Action<O>, O> void call(Class<A> action, StreamObserver<O> stream) {
        invoke(() -> getBean(action).execute(), stream);
    }

    default <A extends Action1<O, I1>, O, I1> void call(Class<A> action, I1 t1, StreamObserver<O> stream) {
        invoke(() -> getBean(action).execute(t1), stream);
    }

    default <A extends Action2<O, I1, I2>, O, I1, I2> void call(Class<A> action, I1 t1, I2 t2, StreamObserver<O> stream) {
        invoke(() -> getBean(action).execute(t1, t2), stream);
    }

    default <A extends Action3<O, I1, I2, I3>, O, I1, I2, I3> void call(Class<A> action, I1 t1, I2 t2, I3 t3, StreamObserver<O> stream) {
        invoke(() -> getBean(action).execute(t1, t2, t3), stream);
    }

    default <A extends Action4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> void call(Class<A> action, I1 t1, I2 t2, I3 t3, I4 t4, StreamObserver<O> stream) {
        invoke(() -> getBean(action).execute(t1, t2, t3, t4), stream);
    }

    private <O> void invoke(GrpcSupplier<O> supplier, StreamObserver<O> stream) {
        try {
            stream.onNext(supplier.get());
            stream.onCompleted();
        } catch (Throwable e) {
            HttpException httpException = e instanceof HttpException ex ? ex : new InternalServerException(e.getMessage());
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(httpException.toString())));
        }
    }

    @FunctionalInterface
    interface GrpcSupplier<T> {
        T get() throws Throwable;
    }
}
