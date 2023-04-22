package net.trellisframework.message.payload;

import net.trellisframework.message.constant.SendMessageStatus;
import net.trellisframework.core.payload.Payload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = false)
public class SendMessageResponse implements Payload {
    private SendMessageStatus status;

    private String message;

    public static SendMessageResponse of(SendMessageStatus status) {
        return of(status, StringUtils.EMPTY);
    }

    public static SendMessageResponse ok() {
        return of(SendMessageStatus.SUCCESS);
    }

    public static SendMessageResponse error(String message) {
        return of(SendMessageStatus.FAILED, message);
    }
}
