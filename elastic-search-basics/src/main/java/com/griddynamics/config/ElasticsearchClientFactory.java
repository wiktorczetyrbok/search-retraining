package com.griddynamics.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

@Factory
public class ElasticsearchClientFactory {

    @Value("${com.griddynamics.es.graduation.project.es-host}")
    private String esHost;

    @Value("${com.griddynamics.es.graduation.project.user:}")
    private String user;

    @Value("${com.griddynamics.es.graduation.project.pass:}")
    private String pass;

    @Singleton
    public RestHighLevelClient esClient() {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(esHost));

        if (user != null && !user.isBlank() && pass != null && !pass.isBlank()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(user, pass));

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(builder);
    }
}
