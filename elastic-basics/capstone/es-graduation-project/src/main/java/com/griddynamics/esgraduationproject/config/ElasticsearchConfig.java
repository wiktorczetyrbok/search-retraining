package com.griddynamics.esgraduationproject.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@ConfigurationProperties("com.griddynamics.es.graduation.project")
@Data
public class ElasticsearchConfig {
    private String esHost;
    private String user;
    private String pass;

    @Bean(name = "esClient")
    public RestHighLevelClient getEsClient() {
        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(esHost));

        // Use credentials if they exist (ES cluster should support authentication)
        if (isNotBlank(user) && isNotBlank(pass)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, pass));
            restClientBuilder.setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(restClientBuilder);
    }
}
