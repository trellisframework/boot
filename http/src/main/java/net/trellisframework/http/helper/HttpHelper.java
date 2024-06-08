package net.trellisframework.http.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.trellisframework.core.log.Logger;
import net.trellisframework.core.message.Messages;
import net.trellisframework.http.exception.HttpErrorMessage;
import net.trellisframework.http.exception.HttpException;
import net.trellisframework.http.exception.ServiceUnavailableException;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StopWatch;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.net.Proxy;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Function;

public class HttpHelper {

    static ObjectMapper mapper;

    public static <T> T getHttpInstance(String uri, Class<T> clazz) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance())
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, List<Interceptor> interceptors) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(interceptors))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, Proxy proxy, Authenticator proxyAuthenticator) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(proxy, proxyAuthenticator))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, Proxy proxy, Authenticator proxyAuthenticator, List<Interceptor> interceptors) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(proxy, proxyAuthenticator, interceptors))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, int connectTimeout, int writeTimeout, int readTimeout) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(connectTimeout, writeTimeout, readTimeout))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, int connectTimeout, int writeTimeout, int readTimeout, List<Interceptor> interceptors) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(connectTimeout, writeTimeout, readTimeout, interceptors))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, int connectTimeout, int writeTimeout, int readTimeout, Proxy proxy, Authenticator proxyAuthenticator) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(connectTimeout, writeTimeout, readTimeout, proxy, proxyAuthenticator))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String uri, Class<T> clazz, int connectTimeout, int writeTimeout, int readTimeout, Proxy proxy, Authenticator proxyAuthenticator, List<Interceptor> interceptors) {
        return new Retrofit.Builder().baseUrl(uri)
                .addConverterFactory(ConverterFactory.create())
                .client(OkHttpProvider.getInstance(connectTimeout, writeTimeout, readTimeout, proxy, proxyAuthenticator, interceptors))
                .build().create(clazz);
    }

    public static <T> T getHttpInstance(String url, Class<T> clazz, Function<OkHttpClient.Builder, OkHttpClient> fn) {
        return getHttpInstance(url, clazz, fn, ConverterFactory.create());
    }

    public static <T> T getHttpInstance(String url, Class<T> clazz, Function<OkHttpClient.Builder, OkHttpClient> fn, Converter.Factory factory) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(factory)
                .client(fn.apply(new OkHttpClient.Builder()))
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
        return execute(func, defaultError, log).body();
    }

    public static <T> Response<T> execute(Call<T> func) throws HttpException {
        return execute(func, false);
    }

    public static <T> Response<T> execute(Call<T> func, Boolean log) throws HttpException {
        return execute(func, new HttpErrorMessage(HttpStatus.SERVICE_UNAVAILABLE, Messages.SERVICE_UNAVAILABLE.getMessage()), log);
    }

    public static <T, E extends HttpErrorMessage> Response<T> execute(Call<T> func, E defaultError) {
        return execute(func, defaultError, false);
    }

    public static <T, E extends HttpErrorMessage> Response<T> execute(Call<T> func, E defaultError, Boolean log) {
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

            if (response.isSuccessful() || isRedirect(response)) {
                return response;
            }
            E errorMessage = parseErrorMessage(response.errorBody(), defaultError);
            throw new HttpException(new HttpErrorMessage(HttpStatus.valueOf(response.code()), errorMessage.getError()));
        } catch (IOException e) {
            Logger.error("CallWebServiceError", e.getMessage(), e);
            throw new ServiceUnavailableException(Messages.SERVICE_UNAVAILABLE);
        }
    }

    private static boolean isRedirect(Response<?> response) {
        return response.code() >= 300 && response.code() < 400;
    }

    private static <T> T parseErrorMessage(String value, T defaultValue) {
        Class<T> clazz = (Class<T>) defaultValue.getClass();
        T response = StringUtils.isEmpty(value) ? defaultValue : fromJson(value, clazz);
        return response == null ? defaultValue : response;
    }

    private static <T> T parseErrorMessage(ResponseBody value, T defaultValue) {
        try {
            String body = value == null ? StringUtils.EMPTY : value.string();
            return parseErrorMessage(body, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        return mapper;
    }

    static <T> T fromJson(String value, Class<T> valueType) {
        try {
            return getMapper().readValue(value, valueType);
        } catch (IOException e) {
            Logger.error("JsonParseException", e.getMessage());
            return null;
        }
    }

}
