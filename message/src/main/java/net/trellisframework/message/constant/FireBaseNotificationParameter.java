package net.trellisframework.message.constant;


public enum FireBaseNotificationParameter {
    SOUND("default"),
    COLOR("#FFFF00");

    private String value;

    FireBaseNotificationParameter(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
