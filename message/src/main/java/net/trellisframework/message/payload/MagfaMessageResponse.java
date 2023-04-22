package net.trellisframework.message.payload;

import net.trellisframework.core.payload.Payload;

public class MagfaMessageResponse implements Payload {
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
