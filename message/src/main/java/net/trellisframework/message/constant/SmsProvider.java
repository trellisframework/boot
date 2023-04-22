package net.trellisframework.message.constant;

import net.trellisframework.message.task.AbstractSendBatchSmsTask;
import net.trellisframework.message.task.SendBatchSmsWithKavehNegarTask;
import net.trellisframework.message.task.SendBatchSmsWithMagfaTask;

public enum SmsProvider {
    MAGFA(SendBatchSmsWithMagfaTask.class),
    KAVEH_NEGAR(SendBatchSmsWithKavehNegarTask.class);

    private final Class<? extends AbstractSendBatchSmsTask> factory;

    public Class<? extends AbstractSendBatchSmsTask> getFactory() {
        return factory;
    }

    SmsProvider(Class<? extends AbstractSendBatchSmsTask> factory) {
        this.factory = factory;
    }
}
