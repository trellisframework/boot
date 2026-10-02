package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.transport.Endpoint;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.http.TransportHttpClient;
import co.elastic.clients.transport.instrumentation.Instrumentation;
import net.trellisframework.core.log.Logger;
import org.springframework.util.StopWatch;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ElasticsearchQueryLoggingInstrumentation implements Instrumentation {

    @Override
    public <TRequest> Context newContext(TRequest request, Endpoint<TRequest, ?, ?> endpoint) {
        return new QueryLoggingContext();
    }

    private static class QueryLoggingContext implements Context {
        private final StopWatch stopWatch = new StopWatch(UUID.randomUUID().toString());

        @Override
        public ThreadScope makeCurrent() {
            return () -> {
            };
        }

        @Override
        public void beforeSendingHttpRequest(TransportHttpClient.Request request, TransportOptions options) {
            stopWatch.start();
            Logger.info("ELASTICSEARCH", "request=%s, method=%s, path=%s, queryParams=%s, body=%s",
                    stopWatch.getId(),
                    request.method(),
                    request.path(),
                    request.queryParams(),
                    bodyAsString(request.body()));
        }

        @Override
        public void afterReceivingHttpResponse(TransportHttpClient.Response response) {
            stopWatch.stop();
            Logger.info("ELASTICSEARCH", "response=%s, status=%d, durationMs=%d",
                    stopWatch.getId(),
                    response.statusCode(),
                    stopWatch.getTotalTimeMillis());
        }

        @Override
        public <TResponse> void afterDecodingApiResponse(TResponse response) {
        }

        @Override
        public void recordException(Throwable throwable) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            Logger.warn(
                    "ELASTICSEARCH",
                    String.format("request=%s failed: durationMs=%d", stopWatch.getId(), stopWatch.getTotalTimeMillis()),
                    throwable
            );
        }

        @Override
        public void close() {
        }

        private static String bodyAsString(Iterable<ByteBuffer> body) {
            if (body == null) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            for (ByteBuffer buffer : body) {
                ByteBuffer copy = buffer.asReadOnlyBuffer();
                byte[] bytes = new byte[copy.remaining()];
                copy.get(bytes);
                result.append(new String(bytes, StandardCharsets.UTF_8));
            }
            return result.toString();
        }
    }
}
