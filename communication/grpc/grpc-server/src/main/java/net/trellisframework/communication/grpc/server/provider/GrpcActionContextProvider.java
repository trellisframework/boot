package net.trellisframework.communication.grpc.server.provider;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.trellisframework.context.action.*;
import net.trellisframework.context.provider.InjectorBeanProvider;
import net.trellisframework.http.exception.HttpException;
import org.checkerframework.checker.units.qual.A;

public interface GrpcActionContextProvider extends InjectorBeanProvider {

    default <A extends Action<O>, O> void call(Class<A> action, StreamObserver<O> stream) {
        try {
            O response = getBean(action).execute();
            stream.onNext(response);
            stream.onCompleted();
        } catch (Throwable e) {
            String error = e instanceof HttpException ? e.toString() : e.getMessage();
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(error)));
        }
    }

    default <A extends Action1<O, I1>, O, I1> void call(Class<A> action, I1 t1, StreamObserver<O> stream) {
        try {
            O response = getBean(action).execute(t1);
            stream.onNext(response);
            stream.onCompleted();
        } catch (Throwable e) {
            String error = e instanceof HttpException ? e.toString() : e.getMessage();
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(error)));
        }
    }

    default <A extends Action2<O, I1, I2>, O, I1, I2> void call(Class<A> action, I1 t1, I2 t2, StreamObserver<O> stream) {
        try {
            O response = getBean(action).execute(t1, t2);
            stream.onNext(response);
            stream.onCompleted();
        } catch (Throwable e) {
            String error = e instanceof HttpException ? e.toString() : e.getMessage();
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(error)));
        }
    }

    default <A extends Action3<O, I1, I2, I3>, O, I1, I2, I3> void call(Class<A> action, I1 t1, I2 t2, I3 t3, StreamObserver<O> stream) {
        try {
            O response = getBean(action).execute(t1, t2, t3);
            stream.onNext(response);
            stream.onCompleted();
        } catch (Throwable e) {
            String error = e instanceof HttpException ? e.toString() : e.getMessage();
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(error)));
        }
    }

    default <A extends Action4<O, I1, I2, I3, I4>, O, I1, I2, I3, I4> void call(Class<A> action, I1 t1, I2 t2, I3 t3, I4 t4, StreamObserver<O> stream) {
        try {
            O response = getBean(action).execute(t1, t2, t3, t4);
            stream.onNext(response);
            stream.onCompleted();
        } catch (Throwable e) {
            String error = e instanceof HttpException ? e.toString() : e.getMessage();
            stream.onError(new StatusRuntimeException(Status.UNKNOWN.withDescription(error)));
        }
    }
}
