package net.trellisframework.http.exception;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import net.trellisframework.core.log.Logger;
import net.trellisframework.http.constant.Messages;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@ControllerAdvice
@Order(value = 100)
public class HttpExceptionHandling {

    @ExceptionHandler({BadGatewayException.class})
    public ResponseEntity<Object> handleBadGatewayException(BadGatewayException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.BAD_GATEWAY, req);
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({BandwidthLimitExceededException.class})
    public ResponseEntity<Object> handleBandwidthLimitExceededException(BandwidthLimitExceededException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, req);
    }

    @ExceptionHandler({ConflictException.class})
    public ResponseEntity<Object> handleConflictException(ConflictException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.CONFLICT, req);
    }

    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.FORBIDDEN, req);
    }

    @ExceptionHandler({GatewayTimeoutException.class})
    public ResponseEntity<Object> handleGatewayTimeoutException(GatewayTimeoutException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.GATEWAY_TIMEOUT, req);
    }

    @ExceptionHandler({InsufficientStorageException.class})
    public ResponseEntity<Object> handleInsufficientStorageException(InsufficientStorageException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.INSUFFICIENT_STORAGE, req);
    }

    @ExceptionHandler({InternalServerException.class})
    public ResponseEntity<Object> handleInternalServerException(InternalServerException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    @ExceptionHandler({NetworkAuthenticationRequiredException.class})
    public ResponseEntity<Object> handleNetworkAuthenticationRequiredException(NetworkAuthenticationRequiredException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, req);
    }

    @ExceptionHandler({NotAcceptableException.class})
    public ResponseEntity<Object> handleAcceptableException(NotAcceptableException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.NOT_ACCEPTABLE, req);
    }

    @ExceptionHandler({NotExtendedException.class})
    public ResponseEntity<Object> handleNotExtendedException(NotExtendedException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.NOT_EXTENDED, req);
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.NOT_FOUND, req);
    }

    @ExceptionHandler({NotImplementedException.class})
    public ResponseEntity<Object> handleNotImplementedException(NotImplementedException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.NOT_IMPLEMENTED, req);
    }

    @ExceptionHandler({PayloadTooLargeException.class})
    public ResponseEntity<Object> handlePayloadTooLargeException(PayloadTooLargeException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.PAYLOAD_TOO_LARGE, req);
    }

    @ExceptionHandler({PaymentRequiredException.class})
    public ResponseEntity<Object> handlePaymentRequiredException(PaymentRequiredException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.PAYMENT_REQUIRED, req);
    }

    @ExceptionHandler({PreConditionRequiredException.class})
    public ResponseEntity<Object> handlePreConditionRequiredException(PreConditionRequiredException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.PRECONDITION_REQUIRED, req);
    }

    @ExceptionHandler({ProcessingException.class})
    public ResponseEntity<Object> handleProcessingException(ProcessingException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.PROCESSING, req);
    }

    @ExceptionHandler({ProxyAuthenticationRequired.class})
    public ResponseEntity<Object> handleProxyAuthenticationRequired(ProxyAuthenticationRequired ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.PROXY_AUTHENTICATION_REQUIRED, req);
    }

    @ExceptionHandler({RequestTimeoutException.class})
    public ResponseEntity<Object> handleRequestTimeoutException(RequestTimeoutException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.REQUEST_TIMEOUT, req);
    }

    @ExceptionHandler({RollBackException.class})
    public ResponseEntity<Object> handleRollBackException(RollBackException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.CONFLICT, req);
    }

    @ExceptionHandler({ServiceUnavailableException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(ServiceUnavailableException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.SERVICE_UNAVAILABLE, req);
    }

    @ExceptionHandler({TokenException.class})
    public ResponseEntity<Object> handleTokenException(TokenException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, req);
    }

    @ExceptionHandler({TooManyRequestsException.class})
    public ResponseEntity<Object> handleToManyRequestsException(TooManyRequestsException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.TOO_MANY_REQUESTS, req);
    }

    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<Object> handleAuthException(UnauthorizedException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.UNAUTHORIZED, req);
    }

    @ExceptionHandler({UnSupportMediaTypeException.class})
    public ResponseEntity<Object> handleUnSupportMediaTypeException(UnSupportMediaTypeException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE, req);
    }

    @ExceptionHandler({UpgradeRequiredException.class})
    public ResponseEntity<Object> handleUpgradeException(UpgradeRequiredException ex, HttpServletRequest req) {
        return handleException(ex, HttpStatus.UPGRADE_REQUIRED, req);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Object> handleBindException(BindException ex, HttpServletRequest req) {
        BindingResult bindingResult = ex.getBindingResult();
        ObjectError error = bindingResult.getAllErrors().parallelStream().findFirst().orElse(null);
        if (error == null || StringUtils.isBlank(error.getDefaultMessage()))
            return new ResponseEntity<>(HttpStatus.OK);
        return extractMessage(error.getDefaultMessage(), HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest req) {
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
        if (constraintViolations == null || ex.getConstraintViolations().isEmpty())
            return new ResponseEntity<>(HttpStatus.OK);
        String firstErrorMessage = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList().stream().findFirst().orElse(StringUtils.EMPTY);
        return extractMessage(firstErrorMessage, HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return extractMessage(Messages.REQUEST_NOT_READABLE.name(), HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({HttpException.class})
    public ResponseEntity<Object> handleHttpException(HttpException ex, HttpServletRequest req) {
        return handleException(ex, ex.getErrorMessage().getHttpStatus(), req);
    }

    protected ResponseEntity<Object> handleException(HttpException ex, HttpStatus httpStatus, HttpServletRequest req) {
        if (StringUtils.isBlank(ex.getErrorMessage().getPath()))
            ex.setPath(req.getServletPath());
        Logger.error("Exception", ex.toString());
        return new ResponseEntity<>(ex.body(), httpStatus);
    }

    private ResponseEntity<Object> extractMessage(String message, HttpStatus defaultStatus, HttpServletRequest req) {
        HttpException httpException = new HttpException(message, Optional.ofNullable(defaultStatus).orElse(HttpStatus.INTERNAL_SERVER_ERROR)) ;
        return handleException(httpException, httpException.getHttpStatus(), req);
    }

    private static <T> T toObject(String value, Class<T> valueType) {
        try {
            ObjectMapper Obj = new ObjectMapper();
            Obj.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return Obj.readValue(value, valueType);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

}
