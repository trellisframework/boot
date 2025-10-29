package net.trellisframework.http.helper;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Set;

public final class RetryInterceptor implements Interceptor {
    public final static Set<Integer> DEFAULT_RETRY_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);
    private final int maxAttempts;
    private final long initialDelayMillis;
    private final long maxDelayMillis;
    private final boolean retryOnConnectionErrors;
    private final Set<Integer> retryStatusCodes;

    public static RetryInterceptor of(long initialDelayMillis, long maxDelayMillis, int maxAttempts) {
        return of(initialDelayMillis, maxDelayMillis, maxAttempts, DEFAULT_RETRY_STATUS_CODES);
    }

    public static RetryInterceptor of(long initialDelayMillis, long maxDelayMillis, int maxAttempts, Set<Integer> retryStatusCodes) {
        return new RetryInterceptor(maxAttempts, initialDelayMillis, maxDelayMillis, true, retryStatusCodes);
    }

    public RetryInterceptor(int maxAttempts, long initialDelayMillis, long maxDelayMillis, boolean retryOnConnectionErrors, Set<Integer> retryStatusCodes) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMillis = initialDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.retryOnConnectionErrors = retryOnConnectionErrors;
        this.retryStatusCodes = retryStatusCodes;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastIo = null;
        long delay = initialDelayMillis;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Response response = chain.proceed(request);
                if (!shouldRetry(response) || attempt == maxAttempts) return response;
                response.close();
            } catch (IOException e) {
                if (!retryOnConnectionErrors || attempt == maxAttempts) throw e;
                lastIo = e;
            }
            sleep(delay);
            delay = nextDelay(delay);
        }
        if (lastIo != null) throw lastIo;
        return chain.proceed(request);
    }

    private boolean shouldRetry(Response response) {
        return retryStatusCodes.contains(response.code());
    }

    private long nextDelay(long prev) {
        long next = prev << 1;
        return Math.min(next, maxDelayMillis);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}