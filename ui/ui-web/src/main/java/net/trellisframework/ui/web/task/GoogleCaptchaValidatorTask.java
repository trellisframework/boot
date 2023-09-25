package net.trellisframework.ui.web.task;

import net.trellisframework.ui.web.helper.WebHelper;
import net.trellisframework.ui.web.infrastructure.GoogleCaptchaService;
import net.trellisframework.ui.web.payload.GoogleCaptchaVerifyResponse;
import net.trellisframework.http.helper.HttpHelper;
import net.trellisframework.context.task.Task2;
import org.springframework.stereotype.Service;

@Service
public class GoogleCaptchaValidatorTask implements Task2<GoogleCaptchaVerifyResponse, String, String>, WebHelper {

    @Override
    public GoogleCaptchaVerifyResponse execute(String secret, String token) {
        return HttpHelper.call(HttpHelper.getHttpInstance("https://www.google.com", GoogleCaptchaService.class).verify(secret, token, getIp()));
    }
}
