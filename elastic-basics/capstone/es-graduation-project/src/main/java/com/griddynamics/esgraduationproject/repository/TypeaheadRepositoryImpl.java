package com.griddynamics.esgraduationproject.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.griddynamics.esgraduationproject.model.TypeaheadServiceRequest;
import com.griddynamics.esgraduationproject.model.TypeaheadServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
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
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.aggregations.metrics.StatsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
@Slf4j
public class TypeaheadRepositoryImpl implements TypeaheadRepository {

    private static final String ITEM_COUNT_AGG = "itemCountRangeAgg";
    private static final String RANK_STATS_SUB_AGG = "RankStatsSubAgg";
    private static final String NAME_FIELD = "name";
    private static final String ITEM_COUNT_FIELD = "itemCount";
    private static final String RANK_FIELD = "rank";
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

    // Mappings, settings and bulk data files
    @Value("${com.griddynamics.es.graduation.project.files.mappings:classpath:elastic/typeaheads/mappings.json}")
    private Resource typeaheadsMappingsFile;
    @Value("${com.griddynamics.es.graduation.project.files.settings:classpath:elastic/typeaheads/settings.json}")
    private Resource typeaheadsSettingsFile;
    @Value("${com.griddynamics.es.graduation.project.files.bulkData:classpath:elastic/typeaheads/bulk_data.txt}")
    private Resource typeaheadsBulkInsertDataFile;

    @Override
    public TypeaheadServiceResponse getAllTypeaheads(TypeaheadServiceRequest request) {
        QueryBuilder mainQuery = QueryBuilders.matchAllQuery();
        return getTypeaheads(mainQuery, request);
    }

    @Override
    public TypeaheadServiceResponse getTypeaheadsByQuery(TypeaheadServiceRequest request) {
        QueryBuilder mainQuery = getQueryByText(request.getTextQuery());
        return getTypeaheads(mainQuery, request);
    }

    private TypeaheadServiceResponse getTypeaheads(QueryBuilder mainQuery, TypeaheadServiceRequest request) {
        // Create search request
        SearchSourceBuilder ssb = new SearchSourceBuilder()
            .query(mainQuery)
            .size(request.getSize());

        // Add sorting and aggregation if necessary
        if (!request.isGetAllRequest()) {
            // Sorting
            ssb.sort(new ScoreSortBuilder().order(SortOrder.DESC)); // sort by _score DESC
            ssb.sort(new FieldSortBuilder(RANK_FIELD).order(SortOrder.DESC)); // sort by rank DESC
            ssb.sort(new FieldSortBuilder(ID_FIELD).order(SortOrder.DESC)); // tie breaker: sort by _id DESC

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
            return new TypeaheadServiceResponse();
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
            .addRange("small", 1.0, 11.0)
            .addRange("medium", 11.0, 101.0)
            .addRange(new RangeAggregator.Range("large", 101.0, null));
        // Stats sub aggregation by the same field
        itemCountAgg.subAggregation(new StatsAggregationBuilder(RANK_STATS_SUB_AGG).field(RANK_FIELD));

        result.add(itemCountAgg);

        return result;
    }

    private TypeaheadServiceResponse getServiceResponse(SearchResponse searchResponse, boolean hasFacets) {
        TypeaheadServiceResponse response = new TypeaheadServiceResponse();

        // Total hits
        response.setTotalHits(searchResponse.getHits().getTotalHits().value);

        // Documents
        List<Map<String, Object>> typeaheads = Arrays.stream(searchResponse.getHits().getHits())
            .map(SearchHit::getSourceAsMap)
            .collect(Collectors.toList());
        response.setTypeaheads(typeaheads);

        // Facets (1 facet by itemCount, if it exists):
        if (hasFacets) {
            Map<String, Map<String, Number>> itemCountAgg = new LinkedHashMap<>();

            ParsedRange parsedRange = searchResponse.getAggregations().get(ITEM_COUNT_AGG);
            parsedRange.getBuckets().stream()
                .sorted(Comparator.comparingDouble(bucket -> (Double) bucket.getFrom()))
                .forEach(bucket -> {
                    String key = bucket.getKeyAsString();
                        Long docCount = bucket.getDocCount();
                        Map<String, Number> bucketValues = new LinkedHashMap<>();
                        bucketValues.put("count", docCount);
                        if (docCount > 0) {
                            ParsedStats rankStatsSubAgg = bucket.getAggregations().get(RANK_STATS_SUB_AGG);
                            bucketValues.put("min rank", rankStatsSubAgg.getMin());
                            bucketValues.put("avg rank", rankStatsSubAgg.getAvg());
                            bucketValues.put("max rank", rankStatsSubAgg.getMax());
                        }

                        itemCountAgg.put(key, bucketValues);
                    });

            response.getFacets().put("Item Counts", itemCountAgg);
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

    @Override
    public void recreateIndex() {
        if (indexExists(indexName)) {
            deleteIndex(indexName);
        }

        String settings = getStrFromResource(typeaheadsSettingsFile);
        String mappings = getStrFromResource(typeaheadsMappingsFile);
        createIndex(indexName, settings, mappings);

        processBulkInsertData(typeaheadsBulkInsertDataFile);
    }

    private boolean indexExists(String indexName) {
        GetIndexRequest existsRequest = new GetIndexRequest(indexName);

        try {
            return esClient.indices().exists(existsRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new RuntimeException("Existence checking is failed for index " + indexName, ex);
        }
    }

    private void deleteIndex(String indexName) {
        try {
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest(indexName);
            AcknowledgedResponse acknowledgedResponse = esClient.indices().delete(deleteRequest, RequestOptions.DEFAULT);
            if (!acknowledgedResponse.isAcknowledged()) {
                log.warn("Index deletion is not acknowledged for indexName: {}", indexName);
            } else {
                log.info("Index {} has been deleted.", indexName);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Deleting of old index version is failed for indexName: " + indexName, ex);
        }
    }

    private void createIndex(String indexName, String settings, String mappings) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
            .mapping(mappings, XContentType.JSON)
            .settings(settings, XContentType.JSON);

        CreateIndexResponse createIndexResponse;
        try {
            createIndexResponse = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new RuntimeException("An error occurred during creating ES index.", ex);
        }

        if (!createIndexResponse.isAcknowledged()) {
            throw new RuntimeException("Creating index not acknowledged for indexName: " + indexName);
        } else {
            log.info("Index {} has been created.", indexName);
        }
    }

    private static String getStrFromResource(Resource resource) {
        try {
            if (!resource.exists()) {
                throw new IllegalArgumentException("File not found: " + resource.getFilename());
            }
            return Resources.toString(resource.getURL(), Charsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Can not read resource file: " + resource.getFilename(), ex);
        }
    }

    private void processBulkInsertData(Resource bulkInsertDataFile) {
        int requestCnt = 0;
        try {
            BulkRequest bulkRequest = new BulkRequest();
            BufferedReader br = new BufferedReader(new InputStreamReader(bulkInsertDataFile.getInputStream()));

            while (br.ready()) {
                String line1 = br.readLine(); // action_and_metadata
                if (isNotEmpty(line1) && br.ready()) {
                    requestCnt++;
                    String line2 = br.readLine();
                    IndexRequest indexRequest = createIndexRequestFromBulkData(line1, line2);
                    if (indexRequest != null) {
                        bulkRequest.add(indexRequest);
                    }
                }
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.getItems().length != requestCnt) {
                log.warn("Only {} out of {} requests have been processed in a bulk request.", bulkResponse.getItems().length, requestCnt);
            } else {
                log.info("{} requests have been processed in a bulk request.", bulkResponse.getItems().length);
            }

            if (bulkResponse.hasFailures()) {
                log.warn("Bulk data processing has failures:\n{}", bulkResponse.buildFailureMessage());
            }
        } catch (IOException ex) {
            log.error("An exception occurred during bulk data processing", ex);
            throw new RuntimeException(ex);
        }
    }

    private IndexRequest createIndexRequestFromBulkData(String line1, String line2) {
        DocWriteRequest.OpType opType = null;
        String esIndexName = null;
        String esId = null;
        boolean isOk = true;

        try {
            String esOpType = objectMapper.readTree(line1).fieldNames().next();
            opType = DocWriteRequest.OpType.fromString(esOpType);

            JsonNode indexJsonNode = objectMapper.readTree(line1).iterator().next().get("_index");
            esIndexName = (indexJsonNode != null ? indexJsonNode.textValue() : indexName);

            JsonNode idJsonNode = objectMapper.readTree(line1).iterator().next().get("_id");
            esId = (idJsonNode != null ? idJsonNode.textValue() : null);
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("An exception occurred during parsing action_and_metadata line in the bulk data file:\n{}\nwith a message:\n{}", line1, ex.getMessage());
            isOk = false;
        }

        try {
            objectMapper.readTree(line2);
        } catch (IOException ex) {
            log.warn("An exception occurred during parsing source line in the bulk data file:\n{}\nwith a message:\n{}", line2, ex.getMessage());
            isOk = false;
        }

        if (isOk) {
            return new IndexRequest(esIndexName)
                .id(esId)
                .opType(opType)
                .source(line2, XContentType.JSON);
        } else {
            return null;
        }
    }
}
