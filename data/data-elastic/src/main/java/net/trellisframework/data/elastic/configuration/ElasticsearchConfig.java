package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.SneakyThrows;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.elastic.constant.Messages;
import net.trellisframework.http.exception.NotFoundException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Optional;


@Configuration
@EnableConfigurationProperties({ElasticsearchProperties.class})
public class ElasticsearchConfig {

    private static ElasticsearchProperties properties;
    private static ElasticsearchClient client;

    public static ElasticsearchProperties getProperties() {
        if (properties == null)
            properties = ApplicationContextProvider.context.getBean(ElasticsearchProperties.class);
        return properties;
    }

    @SneakyThrows
    public static ElasticsearchClient getInstance() {
        if (client == null) {
            ElasticsearchProperties property = getProperties();
            int ConnectionTimeout = Optional.ofNullable(property.getConnectionTimeout()).map(Duration::toMillis).map(Long::intValue).orElse(300000);
            int socketTimeout = Optional.ofNullable(property.getConnectionTimeout()).map(Duration::toMillis).map(Long::intValue).orElse(300000);
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(property.getUsername(), property.getPassword()));
            String uri = property.getUris().stream().findFirst().orElseThrow(() -> new NotFoundException(Messages.ELASTIC_CONFIG_NOT_FOUND));
            SSLContext ssl = SSLContextBuilder.create().loadTrustMaterial(null, new TrustAllStrategy()).build();
            RestClient restClient = RestClient.builder(HttpHost.create(uri))
                    .setRequestConfigCallback(x -> x.setConnectTimeout(ConnectionTimeout).setSocketTimeout(socketTimeout))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder
                                    .setDefaultCredentialsProvider(credentialsProvider)
                                    .setSSLContext(ssl)
                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE))
                    .build();

            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            client = new ElasticsearchClient(transport);
        }
        return client;
    }
}