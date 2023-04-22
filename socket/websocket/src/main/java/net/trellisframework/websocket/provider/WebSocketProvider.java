package net.trellisframework.websocket.provider;

import net.trellisframework.core.application.ApplicationContextProvider;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class WebSocketProvider {

    private static WebSocketProvider instance;
    private static SimpMessagingTemplate template;

    public static WebSocketProvider getInstance() {
        if (ObjectUtils.isEmpty(instance))
            instance = new WebSocketProvider();
        return instance;
    }

    private WebSocketProvider() {
        template = ApplicationContextProvider.context.getBean(SimpMessagingTemplate.class);
    }

    public void broadcast(String destination, Object param) {
        template.convertAndSend(destination, param);
    }

}
