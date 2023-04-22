package net.trellisframework.http.exception;

import net.trellisframework.core.message.MessageHandler;
import org.springframework.http.HttpStatus;

public class InsufficientStorageException extends HttpException {

    public InsufficientStorageException(MessageHandler message) {
        super(message, HttpStatus.INSUFFICIENT_STORAGE);
    }

    public InsufficientStorageException(String message) {
        super(message, HttpStatus.INSUFFICIENT_STORAGE);
    }

    public InsufficientStorageException(ErrorMessage errorMessage) {
        super(new HttpErrorMessage(HttpStatus.INSUFFICIENT_STORAGE, errorMessage.getMessage(), errorMessage.getStatus(), errorMessage.getPath(), errorMessage.getTimestamp()));
    }

}
