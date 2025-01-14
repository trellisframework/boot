package net.trellisframework.stream.core.prodcer;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.stream.core.payload.ProducerMessage;

public class StreamBridge {

    static MessageProducer producer;

    private static MessageProducer getConnection() {
        if (producer == null)
            producer = ApplicationContextProvider.context.getBean(MessageProducer.class);
        return producer;
    }

    public static void send(ProducerMessage<?> message) {
        getConnection().send(message);
    }
}