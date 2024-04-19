package net.trellisframework.data.elastic.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.data.elastic.constant.Messages;
import net.trellisframework.http.exception.NotFoundException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


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

    public static ElasticsearchClient getInstance() {
        if (client == null) {
            ElasticsearchProperties property = getProperties();
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(property.getUsername(), property.getPassword()));
            String uri = property.getUris().stream().findFirst().orElseThrow(() -> new NotFoundException(Messages.ELASTIC_CONFIG_NOT_FOUND));
            RestClient restClient = RestClient.builder(HttpHost.create(uri))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                    .build();

            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            client = new ElasticsearchClient(transport);
        }
        return client;
    }
}