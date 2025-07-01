package com.griddynamics.productindexer.repository;


import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResponse;
import com.griddynamics.productindexer.model.ProductSearchResult;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.griddynamics.productindexer.mapper.ProductMapper.parseProductHit;

@Component
public class ProductSearchRepository {

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
            SearchRequest searchRequest;
            if (request.isBoostIncluded()) {
                FunctionScoreQueryBuilder boostQuery = buildBoostedQuery(boolQuery);

                searchRequest = new SearchRequest(indexName)
                        .source(new SearchSourceBuilder()
                                .query(boostQuery)
                                .size(request.getSize()));
            } else {
                searchRequest = new SearchRequest(indexName)
                        .source(new SearchSourceBuilder()
                                .query(boolQuery)
                                .size(request.getSize()));
            }

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits()) {
                productHits.add(parseProductHit(hit));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ProductSearchResult(productHits.size(), productHits);
    }

    private FunctionScoreQueryBuilder buildBoostedQuery(BoolQueryBuilder boolQuery) {
        return QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("popularity")
                                        .modifier(FieldValueFactorFunction.Modifier.LOG1P)
                                        .factor(0.5f)
                                        .missing(1.0f)
                        )
                }
        );

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

}
