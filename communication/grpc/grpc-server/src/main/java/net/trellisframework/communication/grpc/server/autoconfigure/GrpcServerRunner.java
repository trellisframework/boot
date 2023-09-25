package net.trellisframework.communication.grpc.server.autoconfigure;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import net.trellisframework.communication.grpc.server.properties.GrpcServerProperties;
import net.trellisframework.core.log.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrpcServerRunner implements SmartLifecycle {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Autowired
    private GrpcServicesRegistry registry;

    private Server server;

    private final ServerBuilder<?> serverBuilder;

    private CountDownLatch latch;

    public GrpcServerRunner(GrpcServerProperties properties) {
        serverBuilder = ServerBuilder.forPort(properties.getPort());
    }

    @Override
    public void start() {
        try {
            if (isRunning()) {
                return;
            }
            Logger.info("gRPC", "Starting gRPC Server ...");
            latch = new CountDownLatch(1);
            registry.getBeanNameToServiceBeanMap().forEach((name, srv) -> {
                ServerServiceDefinition serviceDefinition = srv.bindService();
                serverBuilder.addService(serviceDefinition);
            });
            server = serverBuilder.build().start();
            isRunning.set(true);
            startDaemonAwaitThread();
            Logger.info("gRPC", "gRPC Server started, listening on port " + server.getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {

                latch.await();
            } catch (InterruptedException e) {
                Logger.error("gRPC", "gRPC server awaiter interrupted.", e);
            } finally {
                isRunning.set(false);
            }
        });
        awaitThread.setName("grpc-server-awaiter");
        awaitThread.setDaemon(false);
        awaitThread.start();
    }


    @Override
    public void stop() {
        Optional.ofNullable(server).ifPresent(s -> {
            Logger.info("gRPC", "Shutting down gRPC server ...");
            s.shutdown();
            try {
                s.awaitTermination();
            } catch (InterruptedException e) {
                Logger.error("gRPC", "gRPC server interrupted during destroy.", e);
            } finally {
                latch.countDown();
            }
            Logger.info("gRPC", "gRPC server stopped.");
        });
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }
}
