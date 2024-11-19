package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.datatype.jdk8.OptionalLongDeserializer;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Optional;


@Configuration
@EnableConfigurationProperties({ElasticsearchProperties.class})
public class ElasticsearchConfig {

    final ElasticsearchProperties properties;
    static SirenElasticsearchClient client;

    public ElasticsearchConfig(ElasticsearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    @SneakyThrows
    public SirenElasticsearchClient elasticsearchClient() {
        int ConnectionTimeout = Optional.ofNullable(properties.getConnectionTimeout()).map(Duration::toMillis).map(Long::intValue).orElse(300000);
        int socketTimeout = Optional.ofNullable(properties.getConnectionTimeout()).map(Duration::toMillis).map(Long::intValue).orElse(300000);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
        String uri = properties.getUris().stream().findFirst().orElseThrow(() -> new NotFoundException(Messages.ELASTIC_CONFIG_NOT_FOUND));
        SSLContext ssl = SSLContextBuilder.create().loadTrustMaterial(null, new TrustAllStrategy()).build();
        RestClient restClient = RestClient.builder(HttpHost.create(uri))
                .setRequestConfigCallback(x -> x.setConnectTimeout(ConnectionTimeout).setSocketTimeout(socketTimeout))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLContext(ssl)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE))
                .build();
        return new SirenElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
    }

    public static SirenElasticsearchClient getInstance() {
        if (client == null)
            client = ApplicationContextProvider.context.getBean(SirenElasticsearchClient.class);
        return client;
    }
}