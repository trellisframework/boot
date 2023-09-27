package net.trellisframework.message.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FireBaseNotificationParameter {
    SOUND("default"),
    COLOR("#FFFF00");

    private final String value;
}
