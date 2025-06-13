package org.griddynamics.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.griddynamics.mapper.DocumentMapper;
import org.griddynamics.model.ProductResponse;
import org.griddynamics.model.SearchRequest;
import org.griddynamics.model.SearchResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductSearchRepository {

    @Inject
    private Analyzer analyzer;
    @Inject
    private Directory produceDirectory;

    public SearchResponse getProductsByQuery(SearchRequest request) {
        try (DirectoryReader reader = DirectoryReader.open(produceDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields storedFields = searcher.storedFields();

            BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    new String[]{"title", "description", "category"}, analyzer);

            Query parsedTextQuery = queryParser.parse(request.getTextQuery());
            finalQueryBuilder.add(parsedTextQuery, BooleanClause.Occur.MUST);

            request.getFilters().ifPresent(filters -> addFilters(filters, finalQueryBuilder));
            BooleanQuery query = finalQueryBuilder.build();


            return getServiceResponse(searcher.search(query, request.getSize()), storedFields);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFilters(Map<String, String> filters, BooleanQuery.Builder finalQueryBuilder) {
        double minPrice = Double.NEGATIVE_INFINITY;
        double maxPrice = Double.POSITIVE_INFINITY;

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();

            if ("brand".equals(field)) {
                finalQueryBuilder.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.FILTER);
            } else if ("priceFrom".equals(field)) {
                minPrice = Double.parseDouble(value);
            } else if ("priceTo".equals(field)) {
                maxPrice = Double.parseDouble(value);
            } else {
                finalQueryBuilder.add(new TermQuery(new Term("attributes." + field, value)), BooleanClause.Occur.FILTER);
            }
        }

        applyPriceFilter(finalQueryBuilder, minPrice, maxPrice);
    }

    private void applyPriceFilter(BooleanQuery.Builder finalQueryBuilder, double minPrice, double maxPrice) {
        if (minPrice != Double.NEGATIVE_INFINITY || maxPrice != Double.POSITIVE_INFINITY) {
            Query priceRangeQuery = DoublePoint.newRangeQuery("price", minPrice, maxPrice);
            finalQueryBuilder.add(priceRangeQuery, BooleanClause.Occur.FILTER);
        }
    }

    private SearchResponse getServiceResponse(TopDocs topDocs, StoredFields storedFields) {
        long totalHits = topDocs.totalHits.value;

        List<ProductResponse> products = Arrays.stream(topDocs.scoreDocs)
                .map(scoreDoc -> DocumentMapper.mapToResponse(scoreDoc, storedFields))
                .collect(Collectors.toList());
        return new SearchResponse(totalHits, products);
    }
}
