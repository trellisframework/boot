package net.trellisframework.data.sql.exception;

import net.trellisframework.http.exception.ConflictException;
import net.trellisframework.http.exception.ErrorMessage;
import net.trellisframework.http.exception.HttpExceptionHandling;
import net.trellisframework.core.message.Messages;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.StaleObjectStateException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
@Order(value = 99)
public class DataExceptionHandling extends HttpExceptionHandling {

    @ExceptionHandler({TransactionSystemException.class})
    public ResponseEntity<?> handleTransactionSystemException(TransactionSystemException ex, HttpServletRequest req) {
        if (ex.getRootCause() instanceof ConstraintViolationException) {
            return handleConstraintViolationException((ConstraintViolationException) ex.getRootCause(), req);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ExceptionHandler({StaleObjectStateException.class})
    public ResponseEntity<?> handleUpgradeException(StaleObjectStateException ex, HttpServletRequest req) {
        Integer code = !StringUtils.isBlank(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) && NumberUtils.isDigits(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) ? Integer.valueOf(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) : null;
        return handleConflictException(new ConflictException(new ErrorMessage(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage(ex.getIdentifier(), ex.getEntityName()), code)), req);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<?> handleUpgradeException(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        Integer code = !StringUtils.isBlank(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) && NumberUtils.isDigits(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) ? Integer.valueOf(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage("code")) : null;
        return handleConflictException(new ConflictException(new ErrorMessage(Messages.ROW_WAS_CHANGED_BY_ANOTHER_TRANSACTION.getMessage(ex.getIdentifier(), ex.getPersistentClassName()), code)), req);
    }
}
