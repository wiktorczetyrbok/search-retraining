package com.griddynamics.productindexer.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.griddynamics.productindexer.model.SignalEvent;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
@Slf4j
public class SignalIndexer {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Value("${com.griddynamics.es.signals.index}")
    private String signalsIndex;

    public SignalIndexer(RestHighLevelClient client) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    public void indexSignals(Optional<SignalEvent> optionalSignalEvent) throws IOException {
        if (optionalSignalEvent.isPresent()) {
            indexSingleSignal(optionalSignalEvent.get());
        } else {
            indexBulkSignalsFromFile();
        }
    }

    private void indexSingleSignal(SignalEvent signalEvent) throws IOException {
        IndexRequest indexRequest = new IndexRequest(signalsIndex)
                .source(objectMapper.writeValueAsString(signalEvent), XContentType.JSON);

        client.index(indexRequest, RequestOptions.DEFAULT);
        log.info("Indexed single signal event: {}", signalEvent);
    }

    private void indexBulkSignalsFromFile() throws IOException {
        try (InputStream inputStream = new ClassPathResource("elastic/products/clickstream.json").getInputStream()) {
            JsonNode jsonArray = objectMapper.readTree(inputStream);
            BulkRequest bulkRequest = new BulkRequest();
            int count = 0;

            for (JsonNode originalNode : jsonArray) {
                ObjectNode mappedNode = objectMapper.createObjectNode();
                mappedNode.set("query", originalNode.get("query"));
                mappedNode.set("category", originalNode.get("category"));
                mappedNode.set("shownProductIds", originalNode.get("shownProductIds"));
                mappedNode.set("productId", originalNode.get("productId"));
                mappedNode.set("position", originalNode.get("position"));
                mappedNode.set("timestamp", originalNode.get("timestamp"));
                mappedNode.set("eventType", originalNode.get("eventType"));

                IndexRequest indexRequest = new IndexRequest(signalsIndex)
                        .source(objectMapper.writeValueAsString(mappedNode), XContentType.JSON);

                bulkRequest.add(indexRequest);
                count++;
            }

            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.info("Successfully indexed {} events from clickstream.json into '{}'", count, signalsIndex);
        } catch (IOException e) {
            log.error("Error reading or indexing clickstream.json", e);
            throw e;
        }
    }
}
