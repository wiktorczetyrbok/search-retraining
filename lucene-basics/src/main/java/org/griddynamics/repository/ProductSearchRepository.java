package org.griddynamics.repository;

import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.griddynamics.model.ProductResponse;
import org.griddynamics.model.ProductSearchRequest;
import org.griddynamics.model.ProductSearchResponse;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class  ProductSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchRepository.class);

    @Inject
    Directory produceDirectory;

    @Inject
    Analyzer analyzer;

    public ProductSearchResponse getProductsByQuery(ProductSearchRequest request) {
        try (DirectoryReader reader = DirectoryReader.open(produceDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields storedFields = searcher.storedFields();

            BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
                    new String[]{"title", "description", "category"},
                    analyzer
            );
            Query parsedTextQuery = queryParser.parse(request.textQuery());
            finalQueryBuilder.add(parsedTextQuery, BooleanClause.Occur.MUST);

            request.filters().ifPresent(filters -> addFilters(filters, finalQueryBuilder));

            BooleanQuery query = finalQueryBuilder.build();
            TopDocs topDocs = searcher.search(query, request.size());

            log.info("Found {} document(s) for the query: {}", topDocs.totalHits.value, query);

            return getServiceResponse(topDocs, storedFields);
        } catch (IOException e) {
            log.error("Cannot read index", e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error("Cannot parse query: {}", request.textQuery(), e);
            throw new RuntimeException(e);
        }
    }

    private void addFilters(Map<String, String> filters, BooleanQuery.Builder finalQueryBuilder) {
        double minPrice = Double.NEGATIVE_INFINITY;
        double maxPrice = Double.POSITIVE_INFINITY;

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();

            switch (field) {
                case "brand" -> finalQueryBuilder.add(new TermQuery(new Term(field, value)), BooleanClause.Occur.FILTER);
                case "priceFrom" -> minPrice = Double.parseDouble(value);
                case "priceTo" -> maxPrice = Double.parseDouble(value);
                default -> finalQueryBuilder.add(
                        new TermQuery(new Term("attributes." + field, value)), BooleanClause.Occur.FILTER
                );
            }
        }

        if (minPrice != Double.NEGATIVE_INFINITY || maxPrice != Double.POSITIVE_INFINITY) {
            Query priceRangeQuery = DoublePoint.newRangeQuery("price", minPrice, maxPrice);
            finalQueryBuilder.add(priceRangeQuery, BooleanClause.Occur.FILTER);
        }
    }

    private ProductSearchResponse getServiceResponse(TopDocs topDocs, StoredFields storedFields) {
        long totalHits = topDocs.totalHits.value;

        List<ProductResponse> products = Arrays.stream(topDocs.scoreDocs)
                .map(sd -> {
                    int docId = sd.doc;
                    float score = sd.score;

                    try {
                        Document doc = storedFields.document(docId);

                        Map<String, String> attributes = doc.getFields().stream()
                                .filter(f -> f.name().startsWith("attributes."))
                                .collect(Collectors.groupingBy(
                                        f -> f.name().substring("attributes.".length()),
                                        Collectors.mapping(IndexableField::stringValue, Collectors.joining(","))
                                ));

                        return ProductResponse.builder()
                                .score(score)
                                .id(doc.get("id"))
                                .title(doc.get("title"))
                                .description(doc.get("description"))
                                .price(doc.get("price"))
                                .currencyCode(doc.get("currencyCode"))
                                .category(doc.get("category"))
                                .attributes(attributes)
                                .build();
                    } catch (IOException e) {
                        log.error("Cannot read docId: {}", docId, e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        return new ProductSearchResponse(totalHits, products);
    }
}
