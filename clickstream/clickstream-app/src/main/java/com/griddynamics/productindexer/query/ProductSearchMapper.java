package com.griddynamics.productindexer.query;

import com.griddynamics.productindexer.model.FacetBucket;
import com.griddynamics.productindexer.model.ProductSearchResponse;
import com.griddynamics.productindexer.model.ProductSearchResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductSearchMapper {

    public static ProductSearchResult mapToResult(SearchResponse searchResponse, boolean hasFacets) {
        ProductSearchResult response = new ProductSearchResult();
        response.setTotalHits((int) searchResponse.getHits().getTotalHits().value);

        List<ProductSearchResponse> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(hit -> {
                    Map<String, Object> source = hit.getSourceAsMap();
                    return ProductSearchResponse.builder()
                            .score(hit.getScore())
                            .id(String.valueOf(source.get("id")))
                            .title((String) source.get("title"))
                            .description((String) source.get("description"))
                            .price((String) source.get("price"))
                            .currencyCode((String) source.get("currencyCode"))
                            .category((String) source.get("category"))
                            .attributes(castToMapStringString(source.get("attributes")))
                            .popularity(parseDouble(source.get("popularity")))
                            .build();
                })
                .collect(Collectors.toList());

        response.setProducts(products);

        if (hasFacets && searchResponse.getAggregations() != null) {
            Map<String, List<FacetBucket>> facets = new LinkedHashMap<>();

            ParsedTerms brandAgg = searchResponse.getAggregations().get("brand");
            if (brandAgg != null) {
                facets.put("brand", brandAgg.getBuckets().stream()
                        .map(b -> new FacetBucket(b.getKeyAsString(), b.getDocCount()))
                        .collect(Collectors.toList()));
            }

            ParsedRange priceAgg = searchResponse.getAggregations().get("price");
            if (priceAgg != null) {
                facets.put("price", priceAgg.getBuckets().stream()
                        .map(b -> new FacetBucket(b.getKeyAsString(), b.getDocCount()))
                        .collect(Collectors.toList()));
            }

            ParsedNested skusNested = searchResponse.getAggregations().get("skus_nested");
            if (skusNested != null) {
                ParsedTerms skusColor = skusNested.getAggregations().get("skus_color");
                if (skusColor != null) {
                    facets.put("color", skusColor.getBuckets().stream()
                            .map(b -> new FacetBucket(b.getKeyAsString(),
                                    ((ParsedReverseNested) b.getAggregations().get("back_to_product")).getDocCount()))
                            .collect(Collectors.toList()));
                }
            }

            ParsedNested skusNestedSize = searchResponse.getAggregations().get("skus_nested_size");
            if (skusNestedSize != null) {
                ParsedTerms skusSize = skusNestedSize.getAggregations().get("skus_size");
                if (skusSize != null) {
                    facets.put("size", skusSize.getBuckets().stream()
                            .map(b -> new FacetBucket(b.getKeyAsString(),
                                    ((ParsedReverseNested) b.getAggregations().get("back_to_product")).getDocCount()))
                            .collect(Collectors.toList()));
                }
            }

            response.setFacets(facets);
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> castToMapStringString(Object obj) {
        if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).entrySet().stream()
                    .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (String) e.getValue()
                    ));
        }
        return Collections.emptyMap();
    }

    private static Double parseDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}

