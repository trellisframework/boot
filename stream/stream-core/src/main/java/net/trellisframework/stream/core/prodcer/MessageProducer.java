package net.trellisframework.stream.core.prodcer;

import net.trellisframework.stream.core.payload.ProducerMessage;

public interface MessageProducer {

    void send(ProducerMessage<?> value);

}
