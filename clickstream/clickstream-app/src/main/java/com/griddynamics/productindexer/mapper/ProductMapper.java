package com.griddynamics.productindexer.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griddynamics.productindexer.model.ProductSearchResponse;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProductMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static ProductSearchResponse parseProductHit(SearchHit hit) {
        try {
            JsonNode source = objectMapper.readTree(hit.getSourceAsString());
            String id = source.path("id").asText();
            String title = source.path("title").asText();
            String description = source.path("description").asText();
            Float score = hit.getScore();
            String price = source.path("priceInfo").path("price").asText();
            String currencyCode = source.path("priceInfo").path("currencyCode").asText();
            Double popularity = source.path("popularity").asDouble();
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
                    .popularity(popularity)
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
