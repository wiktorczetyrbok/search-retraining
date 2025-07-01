package com.griddynamics.productindexer.repository;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SignalAggregator {

    private final RestHighLevelClient client;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String productIndex;

    @Value("${com.griddynamics.es.signals.index}")
    private String signalsIndex;

    public SignalAggregator(RestHighLevelClient client) {
        this.client = client;
    }

    public void aggregateAndUpdatePopularity() throws IOException {
        Map<String, Double> weightedPopularity = aggregateWeightedClickScores();
        updateProductPopularity(weightedPopularity);
    }

    public Map<String, Double> aggregateWeightedClickScores() throws IOException {
        SearchRequest searchRequest = new SearchRequest(signalsIndex);
        searchRequest.source(new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(10000));

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        Map<String, Double> scores = evaluateScores(response);

        return scores;
    }

    public Map<String, Double> evaluateScores(SearchResponse response) {
        Map<String, Double> scores = new HashMap<>();
        long now = Instant.now().toEpochMilli();

        for (SearchHit hit : response.getHits()) {
            Map<String, Object> doc = hit.getSourceAsMap();
            String productId = (String) doc.get("productId");
            String eventType = (String) doc.get("eventType");
            String timestampStr = (String) doc.get("timestamp");

            long eventTime = Instant.parse(timestampStr).toEpochMilli();
            long ageInDays = Duration.ofMillis(now - eventTime).toDays();

            double decay = Math.exp(-0.01 * ageInDays);
            double weight;
            if ("click".equalsIgnoreCase(eventType)) {
                weight = 1.0;
            } else if ("purchase".equalsIgnoreCase(eventType)) {
                weight = 5.0;
            } else {
                weight = 0.0;
            }

            double score = weight * decay;
            scores.merge(productId, score, Double::sum);
        }

        log.info("Aggregated weighted popularity for {} products", scores.size());
        return scores;
    }


    public void updateProductPopularity(Map<String, Double> popularityMap) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();

        for (Map.Entry<String, Double> entry : popularityMap.entrySet()) {
            String productId = entry.getKey();
            double popularity = entry.getValue();

            UpdateRequest updateRequest = new UpdateRequest(productIndex, productId)
                    .doc(Map.of("popularity", popularity))
                    .docAsUpsert(true);

            bulkRequest.add(updateRequest);
        }

        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("Updated popularity for {} products", popularityMap.size());
    }
}

