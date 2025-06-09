package com.griddynamics.productsearchservice.repository;

import com.griddynamics.productsearchservice.model.FacetBucket;
import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    @Override
    public ProductSearchResponse getAllProducts(ProductSearchRequest request) {
        QueryBuilder query = QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery()); // return nothing
        return getProducts(query, request);
    }

    @Override
    public ProductSearchResponse getProductsByQuery(ProductSearchRequest request) {
        QueryBuilder query = getQueryByText(request.getTextQuery());
        return getProducts(query, request);
    }

    private ProductSearchResponse getProducts(QueryBuilder query, ProductSearchRequest request) {
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        int page = Optional.ofNullable(request.getPage()).orElse(0);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .from(page * size)
                .size(size)
                .sort(new ScoreSortBuilder().order(SortOrder.DESC))
                .sort(new FieldSortBuilder("_id").order(SortOrder.DESC));

        if (!request.isGetAllRequest()) {
            createAggs().forEach(ssb::aggregation);
        }

        SearchRequest searchRequest = new SearchRequest(indexName).source(ssb);
        try {
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            return getServiceResponse(response, !request.isGetAllRequest());
        } catch (IOException e) {
            log.error("Error executing search", e);
            return new ProductSearchResponse();
        }
    }

    private static final Set<String> SIZE_TOKENS = Set.of("xxs", "xs", "s", "m", "l", "xl", "xxl", "xxxl");
    private static final Set<String> COLOR_TOKENS = Set.of("Green", "Black", "White", "Blue", "yellow", "red", "brown", "orange", "grey");

    private QueryBuilder getQueryByText(String textQuery) {
        if (textQuery == null || textQuery.trim().isEmpty()) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
        }

        List<String> tokens = Arrays.stream(textQuery.toLowerCase().split("\\s+")).collect(Collectors.toList());
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        for (String token : tokens) {
            if (SIZE_TOKENS.contains(token)) {
                boolQuery.must(QueryBuilders.termQuery("skus.size.keyword", token).boost(2f));
            } else if (COLOR_TOKENS.contains(token)) {
                boolQuery.must(QueryBuilders.termQuery("skus.color.keyword", token).boost(3f));
            } else {
                boolQuery.must(QueryBuilders.multiMatchQuery(token)
                        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                        .fields(Map.of("brand", 1f, "name", 1f))
                        .operator(Operator.AND));

                boolQuery.should(QueryBuilders.multiMatchQuery(textQuery)
                        .type(MultiMatchQueryBuilder.Type.PHRASE)
                        .fields(Map.of("brand.shingles", 5f, "name.shingles", 5f)));
            }
        }

        return boolQuery;
    }

    private List<AggregationBuilder> createAggs() {
        List<AggregationBuilder> aggs = new ArrayList<>();

        aggs.add(AggregationBuilders.terms("brand")
                .field("brand.keyword")
                .size(20)
                .order(BucketOrder.count(false)));

        aggs.add(AggregationBuilders.range("price")
                .field("itemCount")
                .keyed(true)
                .addRange("Cheap", 0, 100)
                .addRange("Average", 100, 500)
                .addRange(new RangeAggregator.Range("Expensive", 400.0, null)));


        aggs.add(AggregationBuilders.nested("skus_nested", "skus")
                .subAggregation(AggregationBuilders.terms("skus_color")
                        .field("skus.color.keyword")
                        .size(20)
                        .order(BucketOrder.count(false))
                        .subAggregation(new ReverseNestedAggregationBuilder("back_to_product"))));

        aggs.add(AggregationBuilders.nested("skus_nested_size", "skus")
                .subAggregation(AggregationBuilders.terms("skus_size")
                        .field("skus.size.keyword")
                        .size(20)
                        .order(BucketOrder.count(false))
                        .subAggregation(new ReverseNestedAggregationBuilder("back_to_product"))));

        return aggs;
    }

    private ProductSearchResponse getServiceResponse(SearchResponse searchResponse, boolean hasFacets) {
        ProductSearchResponse response = new ProductSearchResponse();
        response.setTotalHits(searchResponse.getHits().getTotalHits().value);

        List<Map<String, Object>> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .collect(Collectors.toList());
        response.setProducts(products);

        if (hasFacets) {
            Map<String, List<FacetBucket>> facets = new LinkedHashMap<>();

            ParsedTerms brandAgg = searchResponse.getAggregations().get("brand");
            List<FacetBucket> brandBuckets = brandAgg.getBuckets().stream()
                    .map(b -> new FacetBucket(b.getKeyAsString(), b.getDocCount()))
                    .collect(Collectors.toList());
            facets.put("brand", brandBuckets);

            ParsedRange priceAgg = searchResponse.getAggregations().get("price");
            List<FacetBucket> priceBuckets = priceAgg.getBuckets().stream()
                    .map(b -> new FacetBucket(b.getKeyAsString(), b.getDocCount()))
                    .collect(Collectors.toList());
            facets.put("price", priceBuckets);

            ParsedNested skusNested = searchResponse.getAggregations().get("skus_nested");
            ParsedTerms skusColor = skusNested.getAggregations().get("skus_color");
            List<FacetBucket> colorBuckets = skusColor.getBuckets().stream()
                    .map(b -> new FacetBucket(b.getKeyAsString(),
                            ((ParsedReverseNested) b.getAggregations().get("back_to_product")).getDocCount()))
                    .collect(Collectors.toList());
            facets.put("color", colorBuckets);

            ParsedNested skusNestedSize = searchResponse.getAggregations().get("skus_nested_size");
            ParsedTerms skusSize = skusNestedSize.getAggregations().get("skus_size");
            List<FacetBucket> sizeBuckets = skusSize.getBuckets().stream()
                    .map(b -> new FacetBucket(b.getKeyAsString(),
                            ((ParsedReverseNested) b.getAggregations().get("back_to_product")).getDocCount())).collect(Collectors.toList());
            facets.put("size", sizeBuckets);

            response.setFacets(facets);
        }

        return response;
    }
}
