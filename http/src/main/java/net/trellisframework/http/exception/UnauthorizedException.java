package net.trellisframework.http.exception;

import net.trellisframework.core.message.MessageHandler;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends HttpException {

    public UnauthorizedException(MessageHandler message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(ErrorMessage errorMessage) {
        super(new HttpErrorMessage(HttpStatus.UNAUTHORIZED, errorMessage.getMessage(), errorMessage.getStatus(), errorMessage.getPath(), errorMessage.getTimestamp()));
    }

}
