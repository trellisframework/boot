package net.trellisframework.context.rule;

import net.trellisframework.context.provider.ActionContextProvider;
import org.springframework.http.HttpStatus;

public abstract class ConstraintRule<T> extends AbstractRule<T> implements ActionContextProvider {

    public abstract String message(T t);

    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    public boolean isEnable() {
        return true;
    }

}
