package net.trellisframework.http.exception;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.trellisframework.core.message.MessageHelper;
import net.trellisframework.core.payload.Payload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class HttpErrorMessage implements Payload {
    private Date timestamp;

    private HttpStatus httpStatus;

    private Integer status;

    private String error;

    private String path;

    public HttpErrorMessage(HttpStatus httpStatus, String message) {
        this(httpStatus, message, null, StringUtils.EMPTY);
    }

    public HttpErrorMessage(HttpStatus httpStatus, String message, Integer status) {
        this(httpStatus, message, status, StringUtils.EMPTY);
    }

    public HttpErrorMessage(HttpStatus httpStatus, String message, Integer status, String path) {
        this(httpStatus, message, status, path, new Date());
    }

    public HttpErrorMessage(HttpStatus httpStatus, String message, Integer status, String path, Date timestamp) {
        List<String> messages = Arrays.asList(message.split(" "));
        String originalMessage = messages.stream().findFirst().orElse(StringUtils.EMPTY);
        String[] parameters = messages.size() < 2 ? new String[0] : messages.subList(1, messages.size()).toArray(new String[messages.size() - 1]);
        String translatedMessage = MessageHelper.getMessage(originalMessage, (Object[]) parameters);
        status = status == null ? MessageHelper.getCode(message) : status;
        this.httpStatus = httpStatus;
        this.timestamp = timestamp;
        this.status = Integer.valueOf(String.valueOf(httpStatus.value()) + (status == null ? StringUtils.EMPTY : status));
        this.error = StringUtils.isEmpty(translatedMessage) ? message.replace('_', ' ').toLowerCase() : translatedMessage;
        this.path = path;
    }

    public Map<String, Serializable> body() {
        return ImmutableMap.of(
                "timestamp", timestamp,
                "status", status,
                "error", error,
                "path", path
        );
    }
}
