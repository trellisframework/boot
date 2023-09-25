package net.trellisframework.communication.grpc.client.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.http.exception.HttpErrorMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MethodInfo implements Payload {
    private String name;
    private Class<? extends HttpErrorMessage> exception;
}
