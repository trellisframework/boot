package net.trellisframework.message.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.trellisframework.message.task.AbstractSendBatchSmsTask;
import net.trellisframework.message.task.SendBatchSmsWithKavehNegarTask;
import net.trellisframework.message.task.SendBatchSmsWithMagfaTask;

@Getter
@AllArgsConstructor
public enum SmsProvider {
    MAGFA(SendBatchSmsWithMagfaTask.class),
    KAVEH_NEGAR(SendBatchSmsWithKavehNegarTask.class);

    private final Class<? extends AbstractSendBatchSmsTask> factory;
}
