package net.trellisframework.message.action;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import feign.FeignException;
import net.trellisframework.context.action.Action1;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.message.payload.SendMailByMailgunRequest;
import net.trellisframework.message.payload.SendMailByMailgunResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SendMailByMailgunAction implements Action1<SendMailByMailgunResponse, SendMailByMailgunRequest> {

    @Override
    public SendMailByMailgunResponse execute(SendMailByMailgunRequest request) {
        try {
            MailgunMessagesApi api = MailgunClient.config(request.getApiKey()).createApi(MailgunMessagesApi.class);
            return plainToClass(api.sendMessage(request.getDomain(),
                    Message.builder()
                            .from(request.getFrom())
                            .to(request.getTo())
                            .subject(request.getSubject())
                            .template(request.getTemplate())
                            .mailgunVariables(request.getVariables())
                            .build()), SendMailByMailgunResponse.class);
        } catch (FeignException exception) {
            throw new HttpException(exception.getMessage(), HttpStatus.resolve(exception.status()));
        }
    }

}
