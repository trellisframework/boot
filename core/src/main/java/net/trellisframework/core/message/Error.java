package net.trellisframework.core.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.core.payload.Payload;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Error implements Payload {
    private Integer code;
    private String message;
}
