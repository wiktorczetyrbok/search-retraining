package com.griddynamics.productindexer.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.griddynamics.productindexer.infrastructure.ProductIndexManager;
import com.griddynamics.productindexer.model.Event;
import com.griddynamics.productindexer.model.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestHighLevelClient esClient;

    @Autowired
    private ProductIndexManager indexManager;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    // Mappings, settings and bulk data files
    @Value("${com.griddynamics.es.graduation.project.files.mappings:classpath:elastic/products/mappings.json}")
    private Resource productsMappingsFile;
    @Value("${com.griddynamics.es.graduation.project.files.settings:classpath:elastic/products/settings.json}")
    private Resource productsSettingsFile;
    @Value("${com.griddynamics.es.graduation.project.files.bulkData:classpath:elastic/products/product_catalog.ndjson}")
    private Resource productsBulkInsertDataFile;

    @Override
    public void recreateIndex() {
        String settings = getStrFromResource(productsSettingsFile);
        String mappings = getStrFromResource(productsMappingsFile);
        String newIndex = indexManager.createTimestampedIndex(indexName, settings, mappings);
        System.out.println(mappings);
        processBulkInsertData(productsBulkInsertDataFile, newIndex);

        indexManager.switchAliasToNewIndex(indexName, newIndex);
        indexManager.refreshIndex(newIndex);
        indexManager.deleteOldIndices(indexName, 3);
    }

    @Override
    public UpdateResult processUpdateEvents(List<Event> events) {
        Set<String> indexedFields = Set.of( "id",
                "name",
                "description",
                "category",
                "brand",
                "price",
                "currency",
                "available",
                "stock");
        Map<String, Event> deduplicated = new LinkedHashMap<>();

        int skippedUnindexed = 0;
        int skippedUnchanged = 0;
        int updated = 0;

        for (Event event : events) {
            if (!indexedFields.contains(event.getField())) {
                skippedUnindexed++;
                continue;
            }

            String key = event.getId() + ":" + event.getField();
            if (deduplicated.containsKey(key)) {
                Event existing = deduplicated.get(key);
                deduplicated.put(key, new Event(event.getId(), event.getField(), existing.getBefore(), event.getAfter()));
            } else {
                deduplicated.put(key, event);
            }
        }

        for (Event event : deduplicated.values()) {
            if (!Objects.equals(event.getBefore(), event.getAfter())) {
                try {
                    UpdateRequest updateRequest = new UpdateRequest(indexName, String.valueOf(event.getId()))
                            .doc(Map.of(event.getField(), event.getAfter()))
                            .docAsUpsert(true);

                    esClient.update(updateRequest, RequestOptions.DEFAULT);
                    updated++;
                    log.info("Updated product {} field '{}' → {}", event.getId(), event.getField(), event.getAfter());
                } catch (Exception e) {
                    log.error("Failed to update product {} field '{}': {}", event.getId(), event.getField(), e.getMessage());
                }
            } else {
                skippedUnchanged++;
                log.info("Skipped product {} field '{}' (no change)", event.getId(), event.getField());
            }
        }

        return new UpdateResult(
                events.size(),
                deduplicated.size(),
                skippedUnchanged,
                skippedUnindexed,
                updated
        );
    }




    private static String getStrFromResource(Resource resource) {
        try {
            if (!resource.exists()) {
                throw new IllegalArgumentException("File not found: " + resource.getFilename());
            }
            return Resources.toString(resource.getURL(), Charsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Can not read resource file: " + resource.getFilename(), ex);
        }
    }

    private void processBulkInsertData(Resource bulkInsertDataFile, String indexName) {
        int requestCnt = 0;
        try {
            BulkRequest bulkRequest = new BulkRequest();
            BufferedReader br = new BufferedReader(new InputStreamReader(bulkInsertDataFile.getInputStream()));

            while (br.ready()) {
                String line1 = br.readLine(); // action_and_metadata
                if (isNotEmpty(line1) && br.ready()) {
                    requestCnt++;
                    String line2 = br.readLine();
                    IndexRequest indexRequest = createIndexRequestFromBulkData(line1, line2, indexName);
                    if (indexRequest != null) {
                        bulkRequest.add(indexRequest);
                    }
                }
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.getItems().length != requestCnt) {
                log.warn("Only {} out of {} requests have been processed in a bulk request.", bulkResponse.getItems().length, requestCnt);
            } else {
                log.info("{} requests have been processed in a bulk request.", bulkResponse.getItems().length);
            }

            if (bulkResponse.hasFailures()) {
                log.warn("Bulk data processing has failures:\n{}", bulkResponse.buildFailureMessage());
            }
        } catch (IOException ex) {
            log.error("An exception occurred during bulk data processing", ex);
            throw new RuntimeException(ex);
        }
    }

    private IndexRequest createIndexRequestFromBulkData(String line1, String line2, String fallbackIndexName) {
        DocWriteRequest.OpType opType = null;
        String esIndexName = fallbackIndexName;
        String esId = null;
        boolean isOk = true;

        try {
            JsonNode metadataNode = objectMapper.readTree(line1);
            String esOpType = metadataNode.fieldNames().next();
            opType = DocWriteRequest.OpType.fromString(esOpType);

            JsonNode metadataContent = metadataNode.iterator().next();
            JsonNode indexNode = metadataContent.get("_index");
            if (indexNode != null && !indexNode.isNull()) {
                esIndexName = indexNode.asText();
            }

            JsonNode idNode = metadataContent.get("_id");
            if (idNode != null && !idNode.isNull()) {
                esId = idNode.asText();
            }
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("An exception occurred during parsing action_and_metadata line in the bulk data file:\n{}\nwith a message:\n{}", line1, ex.getMessage());
            isOk = false;
        }

        try {
            objectMapper.readTree(line2);
        } catch (IOException ex) {
            log.warn("An exception occurred during parsing source line in the bulk data file:\n{}\nwith a message:\n{}", line2, ex.getMessage());
            isOk = false;
        }

        if (isOk) {
            return new IndexRequest(esIndexName)
                    .id(esId)
                    .opType(opType)
                    .source(line2, XContentType.JSON);
        } else {
            return null;
        }
    }
}
