package net.trellisframework.message.action;

import net.trellisframework.context.action.Action3;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SendSmsAction implements Action3<SendMessageResponse, String , String, String> {

    @Override
    public SendMessageResponse execute(String config, String recipient, String message) {
        return call(SendBatchSmsAction.class, config, List.of(recipient), message).stream().findFirst().orElse(null);
    }

}
