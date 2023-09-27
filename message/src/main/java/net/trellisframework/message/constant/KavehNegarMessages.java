package net.trellisframework.message.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.trellisframework.core.message.MessageHandler;

@Getter
@AllArgsConstructor
public enum KavehNegarMessages implements MessageHandler {
    BAD_REQUEST(400),
    FAILED(402),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409);

    private final int value;

    public static KavehNegarMessages of(int v) {
        for (KavehNegarMessages value : values())
            if (v == value.getValue()) return value;
        return FAILED;
    }

}
