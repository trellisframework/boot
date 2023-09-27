package net.trellisframework.message.infrastructure;

import net.trellisframework.http.helper.HttpHelper;
import net.trellisframework.message.payload.SendMailByMailgunResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface MailgunService {

    static MailgunService getInstance() {
        return HttpHelper.getHttpInstance("https://api.mailgun.net", MailgunService.class, 20, 20, 20);
    }

    @FormUrlEncoded
    @POST("/v3/{domain}/messages")
    Call<SendMailByMailgunResponse> send(@Header("api") String apiKey,
                                         @Path("domain") String domain,
                                         @Field("from") String from,
                                         @Field("to") String to,
                                         @Field("subject") String subject,
                                         @Field("template") String template,
                                         @Field("h:X-Mailgun-Variables") Map<String, String> variables);

}
