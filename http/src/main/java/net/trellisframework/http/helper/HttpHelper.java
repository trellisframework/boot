package net.trellisframework.http.helper;

import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StopWatch;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.text.MessageFormat;

public class HttpHelper {

    public static <T> T getHttpInstance(String uri, Class<T> clazz) {
        return new Retrofit.Builder().addConverterFactory(JsonFactory.getInstance()).baseUrl(uri)
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpProvider.getInstance())
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, int connectTimeout, int writeTimeout, int readTimeout) {
        return new Retrofit.Builder().addConverterFactory(JsonFactory.getInstance()).baseUrl(uri)
                .addConverterFactory(GsonConverterFactory.create())
                .client(OkHttpProvider.getInstance(connectTimeout, writeTimeout, readTimeout))
                .build().create(clazz);
    }

    public static <T> T call(Call<T> func) throws HttpException {
        return call(func, false);
    }

    public static <T> T call(Call<T> func, Boolean log) throws HttpException {
        return call(func, new HttpErrorMessage(HttpStatus.SERVICE_UNAVAILABLE, Messages.SERVICE_UNAVAILABLE.getMessage()), log);
    }

    public static <T, E extends HttpErrorMessage> T call(Call<T> func, E defaultError) {
        return call(func, defaultError, false);
    }

    public static <T, E extends HttpErrorMessage> T call(Call<T> func, E defaultError, Boolean log) {
        try {
            Response<T> response;
            if (log) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                response = func.execute();
                stopWatch.stop();
                System.out.println(MessageFormat.format("Call: {0} Time:{1}ms", func.request().url(), stopWatch.getTotalTimeMillis()));
            } else {
                response = func.execute();
            }

            if (response.isSuccessful()) {
                return response.body();
            }
            E errorMessage = parseErrorMessage(response.errorBody(), defaultError);
            throw new HttpException(new HttpErrorMessage(HttpStatus.valueOf(response.code()), errorMessage.getError()));
        } catch (IOException e) {
            Logger.error("CallWebServiceError", e.getMessage(), e);
            throw new ServiceUnavailableException(Messages.SERVICE_UNAVAILABLE);
        }
    }

    public static <T> T parseErrorMessage(String value, T defaultValue) {
        Class<T> clazz = (Class<T>) defaultValue.getClass();
        T response = StringUtils.isEmpty(value) ? defaultValue : JsonFactory.toObject(value, clazz);
        return response == null ? defaultValue : response;
    }

    public static <T> T parseErrorMessage(ResponseBody value, T defaultValue) {
        try {
            String body = value == null ? StringUtils.EMPTY : value.string();
            return parseErrorMessage(body, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
