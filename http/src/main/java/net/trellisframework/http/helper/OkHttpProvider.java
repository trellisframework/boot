package net.trellisframework.http.helper;

import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class OkHttpProvider {
    public static OkHttpClient client;
    static int defaultConnectTimout = 300;
    static int defaultWriteTimout = 300;
    static int defaultReadTimout = 300;

    public OkHttpProvider() {
    }

    public static OkHttpClient getInstance() {
        return getInstance(defaultConnectTimout, defaultWriteTimout, defaultReadTimout);
    }

    public static OkHttpClient getInstance(List<Interceptor> interceptors) {
        return getInstance(defaultConnectTimout, defaultWriteTimout, defaultReadTimout, interceptors);
    }

    public static OkHttpClient getInstance(Proxy proxy, Authenticator proxyAuthenticator) {
        return getInstance(defaultConnectTimout, defaultWriteTimout, defaultReadTimout, proxy, proxyAuthenticator);
    }

    public static OkHttpClient getInstance(Proxy proxy, Authenticator proxyAuthenticator, List<Interceptor> interceptors) {
        return getInstance(defaultConnectTimout, defaultWriteTimout, defaultReadTimout, proxy, proxyAuthenticator, interceptors);
    }

    public static OkHttpClient getInstance(int connectTimeout, int writeTimeout, int readTimeout) {
        return getInstance(connectTimeout, writeTimeout, readTimeout, null);
    }

    public static OkHttpClient getInstance(int connectTimeout, int writeTimeout, int readTimeout, List<Interceptor> interceptors) {
        return getInstance(connectTimeout, writeTimeout, readTimeout, null, null, interceptors);
    }

    public static OkHttpClient getInstance(int connectTimeout, int writeTimeout, int readTimeout, Proxy proxy, Authenticator proxyAuthenticator) {
        return getInstance(connectTimeout, writeTimeout, readTimeout, proxy, proxyAuthenticator, null);
    }

    public static OkHttpClient getInstance(int connectTimeout, int writeTimeout, int readTimeout, Proxy proxy, Authenticator proxyAuthenticator, List<Interceptor> interceptors) {
        if (client == null ||
                TimeUnit.SECONDS.toMillis(connectTimeout) != client.connectTimeoutMillis() ||
                TimeUnit.SECONDS.toMillis(writeTimeout) != client.writeTimeoutMillis() ||
                TimeUnit.SECONDS.toMillis(readTimeout) != client.readTimeoutMillis()
        ) {
            OkHttpClient.Builder builder = new Builder()
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(5, 10L, TimeUnit.SECONDS))
                    .proxy(proxy);

            Optional.ofNullable(interceptors).ifPresent(x -> x.forEach(builder::addInterceptor));
            Optional.ofNullable(proxy).ifPresent(builder::proxy);
            Optional.ofNullable(proxyAuthenticator).ifPresent(builder::proxyAuthenticator);
            client = builder.build();
        }
        return client;
    }

    static {
        client = getInstance();
    }
}
