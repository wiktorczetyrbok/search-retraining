package com.griddynamics.productindexer.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griddynamics.productindexer.model.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductSearchRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    public ProductSearchRepository(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    public ProductSearchResult searchProducts(ProductSearchRequest request) {
        List<ProductSearchResponse> productHits = new ArrayList<>();

        try {
            BoolQueryBuilder boolQuery = buildQuery(request);

            SearchRequest searchRequest = new SearchRequest(indexName)
                    .source(new SearchSourceBuilder()
                            .query(boolQuery)
                            .size(request.getSize() != null ? request.getSize() : 10));

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits()) {
                productHits.add(parseProductHit(hit));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ProductSearchResult(productHits.size(), productHits);
    }

    private BoolQueryBuilder buildQuery(ProductSearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (request.getTextQuery() != null && !request.getTextQuery().isBlank()) {
            MultiMatchQueryBuilder textQuery = QueryBuilders.multiMatchQuery(request.getTextQuery())
                    .field("title", 2.0f)
                    .field("description")
                    .field("categories");
            boolQuery.should(textQuery);
        }

        return boolQuery;
    }

    private ProductSearchResponse parseProductHit(SearchHit hit) {
        try {
            JsonNode source = objectMapper.readTree(hit.getSourceAsString());

            String id = source.path("id").asText();
            String title = source.path("title").asText();
            String description = source.path("description").asText();
            Float score = hit.getScore();
            String price = source.path("priceInfo").path("price").asText();
            String currencyCode = source.path("priceInfo").path("currencyCode").asText();

            String category = null;
            if (source.has("categories") && source.get("categories").isArray() && source.get("categories").size() > 0) {
                category = source.get("categories").get(0).asText();
            }

            Map<String, String> attributes = new HashMap<>();
            if (source.has("attributes")) {
                source.get("attributes").fields().forEachRemaining(entry -> {
                    JsonNode textNode = entry.getValue().path("text");
                    if (textNode.isArray() && textNode.size() > 0) {
                        attributes.put(entry.getKey(), textNode.get(0).asText());
                    }
                });
            }

            return ProductSearchResponse.builder()
                    .id(id)
                    .score(score)
                    .title(title)
                    .description(description)
                    .price(price)
                    .currencyCode(currencyCode)
                    .category(category)
                    .attributes(attributes)
                    .build();

        } catch (IOException e) {
            return ProductSearchResponse.builder()
                    .id("error")
                    .title("Error parsing hit")
                    .description("")
                    .build();
        }
    }

}
