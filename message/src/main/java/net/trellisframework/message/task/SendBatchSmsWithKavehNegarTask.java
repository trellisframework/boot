package net.trellisframework.message.task;

import com.kavenegar.sdk.KavenegarApi;
import com.kavenegar.sdk.models.SendResult;
import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.payload.SendMessageResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SendBatchSmsWithKavehNegarTask extends AbstractSendBatchSmsTask {

    @Override
    public List<SendMessageResponse> execute(MessageProperties.SmsPropertiesDefinition configuration, List<String> recipients, String message) {
        try {
            KavenegarApi api = new KavenegarApi(configuration.getPassword());
            List<SendResult> result = api.send(configuration.getFrom(), recipients, message);
            return Optional.ofNullable(result).orElse(new ArrayList<>()).stream().map(x -> x.getStatus() != 200 ? SendMessageResponse.ok() : SendMessageResponse.error(x.getMessage())).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("KavehNegar error code: " + e.getMessage());
            return null;
        }
    }

}
