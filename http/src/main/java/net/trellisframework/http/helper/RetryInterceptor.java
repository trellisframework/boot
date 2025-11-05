package net.trellisframework.http.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Data
@AllArgsConstructor(staticName = "of")
public final class RetryInterceptor implements Interceptor {
    private final Retry retry;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastIo = null;
        long delay = retry.getInitialDelayMillis();
        for (int attempt = 1; attempt <= retry.getMaxAttempts(); attempt++) {
            try {
                Response response = chain.proceed(request);
                if (!retry.shouldRetry(response.code()) || attempt == retry.getMaxAttempts()) return response;
                response.close();
            } catch (IOException e) {
                if (attempt == retry.getMaxAttempts()) throw e;
                lastIo = e;
            }
            retry.sleep(delay);
            delay = retry.nextDelay(delay);
        }
        if (lastIo != null) throw lastIo;
        return chain.proceed(request);
    }
}