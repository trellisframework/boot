package net.trellisframework.core.message;

import net.trellisframework.core.constant.Language;
import org.apache.commons.lang3.StringUtils;

public interface MessageHandler {
    String name();

    default String getMessage() {
        return getMessage(Language.EN);
    }

    default Integer getCode() {
        return MessageHelper.getCode(name());
    }

    default String getMessage(Object... var1) {
        String result = MessageHelper.getMessage(Language.EN, name(), var1);
        return StringUtils.isEmpty(result) ? this.name().replace('_', ' ').toLowerCase() : result;
    }

    default String getMessage(Language language) {
        String result = MessageHelper.getMessage(language, name());
        return StringUtils.isEmpty(result) ? this.name().replace('_', ' ').toLowerCase() : result;
    }

    default String getMessage(Language language, Object... var1) {
        String result = MessageHelper.getMessage(language, name(), var1);
        return StringUtils.isEmpty(result) ? this.name().replace('_', ' ').toLowerCase() : result;
    }

}
