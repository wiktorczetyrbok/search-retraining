package com.griddynamics.productindexer.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
        Map<String, Long> clickCounts = aggregateClickCounts();
        updateProductPopularity(clickCounts);
    }


    public Map<String, Long> aggregateClickCounts() throws IOException {
        SearchRequest searchRequest = new SearchRequest(signalsIndex);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(new MatchAllQueryBuilder())
                .size(0)
                .aggregation(AggregationBuilders
                        .terms("clicks")
                        .field("productId.keyword")
                        .size(10000));

        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        Terms terms = response.getAggregations().get("clicks");
        Map<String, Long> counts = new HashMap<>();

        for (Terms.Bucket bucket : terms.getBuckets()) {
            counts.put(bucket.getKeyAsString(), bucket.getDocCount());
        }

        log.info("Aggregated {} product click counts", counts.size());
        return counts;
    }

    public void updateProductPopularity(Map<String, Long> clickCounts) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();

        for (Map.Entry<String, Long> entry : clickCounts.entrySet()) {
            String productId = entry.getKey();
            long popularity = entry.getValue();

            UpdateRequest updateRequest = new UpdateRequest(productIndex, productId)
                    .doc(Map.of("popularity", popularity))
                    .docAsUpsert(true);

            bulkRequest.add(updateRequest);
        }

        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("Updated popularity for {} products", clickCounts.size());
    }
}
