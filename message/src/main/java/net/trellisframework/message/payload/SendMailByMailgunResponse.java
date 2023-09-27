package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.core.payload.Payload;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SendMailByMailgunResponse implements Payload {
    private String id;
    private String message;
}
