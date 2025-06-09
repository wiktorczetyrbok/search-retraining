package com.griddynamics.productsearchservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griddynamics.productsearchservice.model.FacetBucket;
import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.elasticsearch.search.aggregations.metrics.StatsAggregationBuilder;
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

    private static final String ITEM_COUNT_AGG = "itemCountRangeAgg";
    private static final String RANK_STATS_SUB_AGG = "RankStatsSubAgg";
    private static final String NAME_FIELD = "name";
    private static final String ITEM_COUNT_FIELD = "itemCount";
    private static final String RANK_FIELD = "rank";
    private static final String NAME_KEYWORD = "name.keyword";
    private static final String ID_FIELD = "_id";


    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    @Value("${com.griddynamics.es.graduation.project.request.fuzziness.startsFromLength.one:4}")
    int fuzzyOneStartsFromLength;
    @Value("${com.griddynamics.es.graduation.project.request.fuzziness.startsFromLength.two:6}")
    int fuzzyTwoStartsFromLength;
    @Value("${com.griddynamics.es.graduation.project.request.fuzziness.boost.zero:1.0}")
    float fuzzyZeroBoost;
    @Value("${com.griddynamics.es.graduation.project.request.fuzziness.boost.one:0.5}")
    float fuzzyOneBoost;
    @Value("${com.griddynamics.es.graduation.project.request.fuzziness.boost.two:0.25}")
    float fuzzyTwoBoost;
    @Value("${com.griddynamics.es.graduation.project.request.prefixQueryBoost:0.9}")
    float prefixQueryBoost;

    @Override
    public ProductSearchResponse getAllProducts(ProductSearchRequest request) {
        QueryBuilder mainQuery = QueryBuilders.matchAllQuery();
        return getProducts(mainQuery, request);
    }

    @Override
    public ProductSearchResponse getProductsByQuery(ProductSearchRequest request) {
        QueryBuilder mainQuery = getQueryByText(request.getTextQuery());
        return getProducts(mainQuery, request);
    }

    private ProductSearchResponse getProducts(QueryBuilder mainQuery, ProductSearchRequest request) {
        // Create search request
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        int page = Optional.ofNullable(request.getPage()).orElse(0);

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(mainQuery)
                .size(request.getSize());

        // Add sorting and aggregation if necessary
        if (!request.isGetAllRequest()) {
            // Sorting
            ssb.sort(new ScoreSortBuilder().order(SortOrder.DESC));
            ssb.sort(new FieldSortBuilder(ID_FIELD).order(SortOrder.DESC));
            ssb.size(size).from(page * size);

            // Aggregation
            List<AggregationBuilder> aggs = createAggs();
            aggs.forEach(ssb::aggregation);
        }

        // Search in ES
        SearchRequest searchRequest = new SearchRequest(indexName).source(ssb);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
            // Build service response
            return getServiceResponse(searchResponse, !request.isGetAllRequest());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return new ProductSearchResponse();
        }
    }

    private List<AggregationBuilder> createAggs() {
        List<AggregationBuilder> result = new ArrayList<>();

        // Facets: 1 range aggregation by itemCount
        RangeAggregationBuilder itemCountAgg = AggregationBuilders
                .range(ITEM_COUNT_AGG)
                .field(ITEM_COUNT_FIELD)
                .keyed(true)
                .addRange(new RangeAggregator.Range("empty", null, 1.0))
                .addRange("small", 1.0, 200.0)
                .addRange("medium", 200.0, 400.0)
                .addRange(new RangeAggregator.Range("large", 400.0, null));
        // Stats sub aggregation by the same field
        itemCountAgg.subAggregation(new StatsAggregationBuilder(RANK_STATS_SUB_AGG).field(RANK_FIELD));

        result.add(itemCountAgg);

        return result;
    }

    private ProductSearchResponse getServiceResponse(SearchResponse searchResponse, boolean hasFacets) {
        ProductSearchResponse response = new ProductSearchResponse();

        // Total hits
        response.setTotalHits(searchResponse.getHits().getTotalHits().value);

        // Products (_source)
        List<Map<String, Object>> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(SearchHit::getSourceAsMap)
                .collect(Collectors.toList());
        response.setProducts(products);

        // Facets
        if (hasFacets) {
            Map<String, List<FacetBucket>> facets = new LinkedHashMap<>();

            ParsedRange parsedRange = searchResponse.getAggregations().get(ITEM_COUNT_AGG); // Example facet
            List<FacetBucket> priceFacetBuckets = parsedRange.getBuckets().stream()
                    .map(bucket -> new FacetBucket(bucket.getKeyAsString(), bucket.getDocCount()))
                    .collect(Collectors.toList());

            facets.put("price", priceFacetBuckets); // Change key name appropriately
            response.setFacets(facets);
        }

        return response;
    }


    private QueryBuilder getQueryByText(String textQuery) {
        List<String> words = Arrays.asList(textQuery.split(" "));
        List<QueryBuilder> mainQueryList = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            int maxLevenshteinDistance = getDistanceByTermLength(word);
            List<QueryBuilder> wordQueries = new ArrayList<>();
            // Queries for all possible Levenshtein distances
            for (int distance = 0; distance <= maxLevenshteinDistance; distance++) {
                float boost = getBoostByDistance(distance);
                if (distance == 0) {
                    wordQueries.add(QueryBuilders.matchQuery(NAME_FIELD, word).boost(boost));
                } else {
                    wordQueries.add(QueryBuilders.matchQuery(NAME_FIELD, word).boost(boost).fuzziness(String.valueOf(distance)));
                }
            }

            // Prefix query for the last word
            if (i == words.size() - 1) {
                wordQueries.add(QueryBuilders.prefixQuery(NAME_FIELD, word.toLowerCase()).boost(prefixQueryBoost));
            }

            // Add all queries for the current word to mainQueryList
            if (wordQueries.size() == 1) {
                mainQueryList.add(wordQueries.get(0));
            } else {
                DisMaxQueryBuilder dmqb = QueryBuilders.disMaxQuery().tieBreaker(1.0f);
                wordQueries.forEach(dmqb::add);
                mainQueryList.add(dmqb);
            }
        }

        // Create result query from mainQueryList
        BoolQueryBuilder result = QueryBuilders.boolQuery();
        mainQueryList.forEach(result::must);
        return result;
    }

    private int getDistanceByTermLength(final String token) {
        return token.length() >= fuzzyTwoStartsFromLength
                ? 2
                : (token.length() >= fuzzyOneStartsFromLength ? 1 : 0);
    }

    private float getBoostByDistance(final int distance) {
        return distance == 0
                ? fuzzyZeroBoost
                : (distance == 1 ? fuzzyOneBoost : fuzzyTwoBoost);
    }
}
