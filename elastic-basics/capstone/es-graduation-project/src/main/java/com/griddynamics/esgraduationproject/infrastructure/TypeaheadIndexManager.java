package com.griddynamics.esgraduationproject.infrastructure;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TypeaheadIndexManager {

    private final RestHighLevelClient esClient;

    public String createTimestampedIndex(String baseAlias, String settings, String mappings) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());
        String newIndexName = baseAlias + "_" + timestamp;

        CreateIndexRequest request = new CreateIndexRequest(newIndexName)
                .settings(settings, XContentType.JSON)
                .mapping(mappings, XContentType.JSON);

        try {
            CreateIndexResponse response = esClient.indices().create(request, RequestOptions.DEFAULT);
            if (!response.isAcknowledged()) {
                throw new RuntimeException("Failed to create index: " + newIndexName);
            }
            return newIndexName;
        } catch (IOException e) {
            throw new RuntimeException("Index creation error: " + newIndexName, e);
        }
    }

    public void switchAliasToNewIndex(String alias, String newIndexName) {
        try {
            GetAliasesRequest getAliasRequest = new GetAliasesRequest(alias);
            GetAliasesResponse aliasResponse = esClient.indices().getAlias(getAliasRequest, RequestOptions.DEFAULT);

            IndicesAliasesRequest request = new IndicesAliasesRequest();

            aliasResponse.getAliases().forEach((index, meta) -> request.addAliasAction(
                    IndicesAliasesRequest.AliasActions.remove().index(index).alias(alias)));

            request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(newIndexName).alias(alias));

            esClient.indices().updateAliases(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Alias switch failed to " + newIndexName, e);
        }
    }

    public void deleteOldIndices(String aliasPrefix, int keepCount) {
        try {
            GetIndexRequest getRequest = new GetIndexRequest(aliasPrefix + "_*");
            String[] all = esClient.indices().get(getRequest, RequestOptions.DEFAULT).getIndices();

            List<String> old = Arrays.stream(all)
                    .filter(name -> name.matches(aliasPrefix + "_\\d{14}"))
                    .sorted(Comparator.reverseOrder()) // newest first
                    .skip(keepCount)
                    .collect(Collectors.toList());

            for (String index : old) {
                System.out.println("Deleting old index: " + index);
                esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            throw new RuntimeException("Old index cleanup failed", e);
        }
    }

    public void refreshIndex(String indexName) {
        try {
            esClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Failed to refresh index " + indexName, e);
        }
    }
}
