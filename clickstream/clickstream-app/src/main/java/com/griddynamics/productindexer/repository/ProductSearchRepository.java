package com.griddynamics.productindexer.repository;


import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static com.griddynamics.productindexer.query.ProductFacetBuilder.buildFacets;
import static com.griddynamics.productindexer.query.ProductSearchMapper.mapToResult;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    public ProductSearchResult getAllProducts(ProductSearchRequest request) {
        QueryBuilder query = QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery()); // return nothing
        return getProducts(query, request);
    }

    public ProductSearchResult getProductsByQuery(ProductSearchRequest request) {
        QueryBuilder query = getQueryByText(request.getTextQuery());


        return getProducts(query, request);
    }

    private ProductSearchResult getProducts(QueryBuilder query, ProductSearchRequest request) {
        int size = Optional.ofNullable(request.getSize()).orElse(10);
        int page = Optional.ofNullable(request.getPage()).orElse(0);
        boolean includeFacets = !request.isGetAllRequest();

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(query)
                .from(page * size)
                .size(size);

        if (!request.isGetAllRequest()) {
            ssb.sort(new ScoreSortBuilder().order(SortOrder.DESC));
            ssb.sort(new FieldSortBuilder("id").order(SortOrder.DESC));
            buildFacets().forEach(ssb::aggregation);
        }

        QueryBuilder finalQuery;
        if (request.isBoostIncluded()) {
            finalQuery = buildBoostedQuery(query);
            ssb.query(finalQuery);
        }

        SearchRequest searchRequest = new SearchRequest(indexName).source(ssb);

        try {
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            return mapToResult(response, includeFacets);
        } catch (IOException e) {
            log.error("Error executing search", e);
            return new ProductSearchResult();
        }
    }

    private static final Set<String> SIZE_TOKENS = Set.of("xxs", "xs", "s", "m", "l", "xl", "xxl", "xxxl");
    private static final Set<String> COLOR_TOKENS = Set.of("green", "black", "white", "blue", "yellow", "red", "brown", "orange", "grey");

    private QueryBuilder getQueryByText(String textQuery) {
        if (textQuery == null || textQuery.trim().isEmpty()) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
        }

        List<String> tokens = Arrays.asList(textQuery.toLowerCase().split("\\s+"));

        BoolQueryBuilder mainBool = QueryBuilders.boolQuery();
        BoolQueryBuilder nestedSkuBool = QueryBuilders.boolQuery();
        boolean hasNested = false;

        for (String token : tokens) {
            if (SIZE_TOKENS.contains(token)) {
                nestedSkuBool.should(QueryBuilders.termQuery("skus.size", token).boost(2f));
                hasNested = true;
            } else if (COLOR_TOKENS.contains(token)) {
                nestedSkuBool.should(QueryBuilders.termQuery("skus.color", token).boost(3f));
                hasNested = true;
            } else {
                mainBool.should(QueryBuilders.multiMatchQuery(token)
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fields(Map.of("brand", 1f, "name", 1f, "description", 0.5f))
                        .operator(Operator.OR));
            }
        }
        if (hasNested) {
            mainBool.filter(QueryBuilders.nestedQuery("skus", nestedSkuBool, ScoreMode.Max));
        }

        mainBool.should(QueryBuilders.multiMatchQuery(textQuery)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .fields(Map.of("brand.shingles", 5f, "name.shingles", 5f)));

        if (!mainBool.should().isEmpty()) {
            mainBool.minimumShouldMatch(1);
        }

        return mainBool;
    }

    private FunctionScoreQueryBuilder buildBoostedQuery(QueryBuilder boolQuery) {
        return QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("popularity")
                                        .modifier(FieldValueFactorFunction.Modifier.LOG1P)
                                        .factor(2f)
                                        .missing(0.5f)
                        )
                }
        );

    }

}
