package net.trellisframework.message.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.payload.Payload;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class EmbeddedData implements Payload {
    private String name;

    private byte[] data;

    private String mimeType;
}