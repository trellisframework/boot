package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.transport.Endpoint;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.http.TransportHttpClient;
import co.elastic.clients.transport.instrumentation.Instrumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ElasticsearchQueryLoggingInstrumentation implements Instrumentation {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchQueryLoggingInstrumentation.class);

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
            log.info("Elasticsearch request:{}, method={}, path={}, queryParams={}, body={}",
                    stopWatch.getId(),
                    request.method(),
                    request.path(),
                    request.queryParams(),
                    bodyAsString(request.body()));
        }

        @Override
        public void afterReceivingHttpResponse(TransportHttpClient.Response response) {
            stopWatch.stop();
            log.info("Elasticsearch response:{}, status={}, durationMs={}",
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
            log.warn("Elasticsearch request={} failed: durationMs={}", stopWatch.getId(),stopWatch.getTotalTimeMillis(), throwable);
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
