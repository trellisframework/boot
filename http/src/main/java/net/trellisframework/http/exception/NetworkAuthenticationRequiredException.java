package net.trellisframework.http.exception;

import net.trellisframework.core.message.MessageHandler;
import org.springframework.http.HttpStatus;

public class NetworkAuthenticationRequiredException extends HttpException {

    public NetworkAuthenticationRequiredException(MessageHandler message) {
        super(message, HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
    }

    public NetworkAuthenticationRequiredException(String message) {
        super(message, HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
    }

    public NetworkAuthenticationRequiredException(ErrorMessage errorMessage) {
        super(new HttpErrorMessage(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, errorMessage.getMessage(), errorMessage.getStatus(), errorMessage.getPath(), errorMessage.getTimestamp()));
    }

}
