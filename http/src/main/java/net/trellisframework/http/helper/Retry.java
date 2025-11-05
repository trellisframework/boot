package net.trellisframework.http.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.trellisframework.core.payload.Payload;

import java.util.Set;

@Data
@AllArgsConstructor(staticName = "of")
public final class Retry implements Payload {
    public final static Set<Integer> DEFAULT_RETRY_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);
    private final long initialDelayMillis;
    private final long maxDelayMillis;
    private final int maxAttempts;
    private final Set<Integer> retryStatusCodes;

    public static Retry of(long initialDelayMillis, long maxDelayMillis, int maxAttempts) {
        return of(initialDelayMillis, maxDelayMillis, maxAttempts, DEFAULT_RETRY_STATUS_CODES);
    }

    boolean shouldRetry(int code) {
        return retryStatusCodes.contains(code);
    }

    long nextDelay(long prev) {
        long next = prev << 1;
        return Math.min(next, maxDelayMillis);
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}