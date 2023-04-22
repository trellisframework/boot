package net.trellisframework.ui.web.infrastructure;

import net.trellisframework.ui.web.payload.GoogleCaptchaVerifyResponse;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface GoogleCaptchaService {
    @FormUrlEncoded
    @POST("/recaptcha/api/siteverify")
    Call<GoogleCaptchaVerifyResponse> verify(@Field("secret") String secret, @Field("response") String response, @Field("remoteip") String ip);
}
