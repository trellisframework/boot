package net.trellisframework.communication.grpc.server.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties("grpc")
@Getter
@Setter
public class GrpcServerProperties implements InitializingBean {
    public static final int DEFAULT_GRPC_PORT = 6565;

    private Integer port = null;

    private SecurityProperties security;

    private RecoveryProperties recovery;

    private NettyServerProperties nettyServer;

    private int startUpPhase = SmartLifecycle.DEFAULT_PHASE;

    private boolean enabled = true;

    private String inProcessServerName;

    private boolean enableReflection = false;

    private int shutdownGrace = 0;

    public Integer getPortOrDefault() {
        return Optional.ofNullable(port).orElse(DEFAULT_GRPC_PORT);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Optional.ofNullable(nettyServer)
                .map(NettyServerProperties::getPrimaryListenAddress)
                .ifPresent(a -> port = a.getPort());
    }

    @Getter
    @Setter
    public static class RecoveryProperties {
        private Integer interceptorOrder;
    }

    @Getter
    @Setter
    public static class SecurityProperties {
        private Resource certChain;
        private Resource privateKey;
        private Auth auth;

        @Getter
        @Setter
        public static class Auth {
            private Integer interceptorOrder;
            private boolean failFast = true;
        }
    }


    @Getter
    @Setter
    public static class NettyServerProperties {
        private boolean onCollisionPreferShadedNetty;
        private Integer flowControlWindow;
        private Integer initialFlowControlWindow;

        private Integer maxConcurrentCallsPerConnection;

        private Duration keepAliveTime;
        private Duration keepAliveTimeout;

        private Duration maxConnectionAge;
        private Duration maxConnectionAgeGrace;
        private Duration maxConnectionIdle;
        private Duration permitKeepAliveTime;

        private DataSize maxInboundMessageSize;
        private DataSize maxInboundMetadataSize;

        private Boolean permitKeepAliveWithoutCalls;

        private InetSocketAddress primaryListenAddress;

        private List<InetSocketAddress> additionalListenAddresses;
    }


}