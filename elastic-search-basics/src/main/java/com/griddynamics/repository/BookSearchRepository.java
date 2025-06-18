package com.griddynamics.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griddynamics.model.BookHit;
import com.griddynamics.model.BookSearchRequest;
import com.griddynamics.model.BookSearchResult;
import com.griddynamics.model.GenreFacet;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Singleton
public class BookSearchRepository {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestHighLevelClient esClient;
    @Value("${com.griddynamics.es.graduation.project.index}")
    private String indexName;

    public BookSearchRepository(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    public BookSearchResult searchBooks(BookSearchRequest request) {
        List<BookHit> bookHits = new ArrayList<>();
        List<GenreFacet> genreFacets = new ArrayList<>();

        try {
            BoolQueryBuilder boolQuery = buildQuery(request);

            SearchRequest searchRequest = new SearchRequest(indexName)
                    .source(new SearchSourceBuilder()
                            .query(boolQuery)
                            .size(10)
                            .aggregation(AggregationBuilders.terms("genres_agg").field("genres")).size(10));

            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits()) {
                bookHits.add(parseBookHit(hit));
            }

            Terms genresAgg = response.getAggregations().get("genres_agg");
            for (Terms.Bucket bucket : genresAgg.getBuckets()) {
                genreFacets.add(new GenreFacet(bucket.getKeyAsString(), bucket.getDocCount()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BookSearchResult(bookHits, genreFacets);
    }

    private BoolQueryBuilder buildQuery(BookSearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (request.getTextQuery() != null && !request.getTextQuery().isBlank()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(request.getTextQuery())
                    .field("title")
                    .field("summary"));
        }

        request.getAuthorFilter().ifPresent(author ->
                boolQuery.filter(QueryBuilders.termQuery("authors.keyword", author))
        );

        request.getGenreFilter().ifPresent(genre ->
                boolQuery.filter(QueryBuilders.termQuery("genres", genre))
        );

        return boolQuery;
    }

    private BookHit parseBookHit(SearchHit hit) {
        try {
            JsonNode source = objectMapper.readTree(hit.getSourceAsString());

            String title = source.path("title").asText();
            String summary = source.path("summary").asText();
            List<String> authors = new ArrayList<>();

            if (source.has("authors") && source.get("authors").isArray()) {
                for (JsonNode node : source.get("authors")) {
                    authors.add(node.asText());
                }
            }
            List<String> genres = new ArrayList<>();
            if (source.has("genres") && source.get("genres").isArray()) {
                for (JsonNode node : source.get("genres")) {
                    genres.add(node.asText());
                }
            }

            String snippet = summary.length() > 100 ? summary.substring(0, 100) + "..." : summary;

            return new BookHit(title, authors, snippet, genres);
        } catch (IOException e) {
            return new BookHit("Invalid", List.of(), "", List.of());
        }
    }


}
