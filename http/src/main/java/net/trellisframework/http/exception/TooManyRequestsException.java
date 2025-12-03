package net.trellisframework.http.exception;

import net.trellisframework.core.message.MessageHandler;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends HttpException {

    public TooManyRequestsException(MessageHandler message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public TooManyRequestsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public TooManyRequestsException(ErrorMessage errorMessage) {
        super(new HttpErrorMessage(HttpStatus.TOO_MANY_REQUESTS, errorMessage.getMessage(), errorMessage.getStatus(), errorMessage.getPath(), errorMessage.getTimestamp()));
    }

}