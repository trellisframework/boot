package net.trellisframework.data.elastic.payload;

import net.trellisframework.core.payload.Payload;

public interface EsPayload extends Payload {
    void setId(String id);
}
