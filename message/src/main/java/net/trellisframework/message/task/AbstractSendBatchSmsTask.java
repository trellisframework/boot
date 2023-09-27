package net.trellisframework.message.task;

import net.trellisframework.context.task.Task3;
import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.payload.SendMessageResponse;

import java.util.List;

public abstract class AbstractSendBatchSmsTask implements Task3<List<SendMessageResponse>, MessageProperties.SmsPropertiesDefinition, List<String>, String> {

}
