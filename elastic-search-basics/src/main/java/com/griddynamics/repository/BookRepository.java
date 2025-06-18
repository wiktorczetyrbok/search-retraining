package com.griddynamics.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.griddynamics.infrastructure.BookIndexManager;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Singleton
public class BookRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = Logger.getLogger(BookRepository.class.getName());

    public BookRepository(RestHighLevelClient esClient, BookIndexManager indexManager, ResourceLoader resourceLoader) {
        this.esClient = esClient;
        this.indexManager = indexManager;
        this.resourceLoader = resourceLoader;
    }

    private final RestHighLevelClient esClient;
    private final BookIndexManager indexManager;
    private final ResourceLoader resourceLoader;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    @Value("${files.mappings:classpath:elastic/mappings.json}")
    private String mappingsPath;

    @Value("${files.settings:classpath:elastic/settings.json}")
    private String settingsPath;

    @Value("${files.bulkData:classpath:elastic/books.ndjson}")
    private String bulkDataPath;

    public void recreateIndex() {
        String settings = getStrFromResource(settingsPath);
        String mappings = getStrFromResource(mappingsPath);
        String newIndex = indexManager.createTimestampedIndex(indexName, settings, mappings);
        processBulkInsertData(bulkDataPath, newIndex);

        indexManager.switchAliasToNewIndex(indexName, newIndex);
        indexManager.refreshIndex(newIndex);
        indexManager.deleteOldIndices(indexName, 1);
    }

    private String getStrFromResource(String path) {
        try {
            Optional<URL> resource = resourceLoader.getResource(path);
            if (resource.isEmpty()) {
                throw new IllegalArgumentException("File not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().openStream(), StandardCharsets.UTF_8))) {
                return reader.lines().reduce("", (a, b) -> a + b + "\n");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read resource: " + path, e);
        }
    }

    private void processBulkInsertData(String path, String indexName) {
        try {
            Optional<URL> resource = resourceLoader.getResource(path);
            if (resource.isEmpty()) {
                throw new IllegalArgumentException("Bulk file not found: " + path);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(resource.get().openStream(), StandardCharsets.UTF_8));
            BulkRequest bulkRequest = new BulkRequest();
            String line;

            while ((line = br.readLine()) != null) {
                if (isNotEmpty(line)) {
                    try {
                        JsonNode originalNode = objectMapper.readTree(line);
                        ObjectNode mappedNode = objectMapper.createObjectNode();
                        mappedNode.set("title", originalNode.get("Title"));
                        mappedNode.set("summary", originalNode.get("Description"));
                        mappedNode.set("authors", originalNode.get("Authors"));
                        mappedNode.set("genres", originalNode.get("Category"));
                        mappedNode.set("publisher", originalNode.get("Publisher"));
                        mappedNode.set("publishYear", originalNode.get("PublishDateYear"));

                        IndexRequest indexRequest = new IndexRequest(indexName)
                                .source(objectMapper.writeValueAsString(mappedNode), XContentType.JSON);

                        bulkRequest.add(indexRequest);
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid JSON or mapping for line: {}", e);
                    }
                }
            }

            if (bulkRequest.numberOfActions() == 0) {
                throw new IllegalStateException("No valid documents found in the NDJSON file.");
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new RuntimeException("Bulk insert had failures: " + bulkResponse.buildFailureMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process bulk data", e);
        }
    }
}
