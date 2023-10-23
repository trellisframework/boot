package net.trellisframework.socket.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties(prefix = "socket.socket-io")
public class SocketIOServerProperties {
    private String host = "0.0.0.0";

    private int port = 9092;

    private boolean reuseAddress = true;
}