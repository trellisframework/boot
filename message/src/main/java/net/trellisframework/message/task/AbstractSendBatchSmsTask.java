package net.trellisframework.message.task;

import net.trellisframework.message.config.SmsPropertiesDefinition;
import net.trellisframework.message.payload.SendMessageResponse;
import net.trellisframework.context.task.Task3;

import java.util.List;

public abstract class AbstractSendBatchSmsTask implements Task3<List<SendMessageResponse>, SmsPropertiesDefinition, List<String>, String> {

}
