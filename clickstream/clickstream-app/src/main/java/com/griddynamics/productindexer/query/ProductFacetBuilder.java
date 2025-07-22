package com.griddynamics.productindexer.query;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductFacetBuilder {

    public static List<AggregationBuilder> buildFacets() {
        List<AggregationBuilder> aggs = new ArrayList<>();

        aggs.add(AggregationBuilders.terms("brand")
                .field("brand.raw")
                .size(20)
                .order(List.of(BucketOrder.count(false), BucketOrder.key(true))));

        aggs.add(AggregationBuilders.range("price")
                .field("price")
                .keyed(true)
                .addRange("Cheap", 0, 100)
                .addRange("Average", 100, 500)
                .addRange(new RangeAggregator.Range("Expensive", 500.0, null)));

        aggs.add(AggregationBuilders.nested("skus_nested", "skus")
                .subAggregation(AggregationBuilders.terms("skus_color")
                        .field("skus.color.raw")
                        .size(20)
                        .order(List.of(BucketOrder.count(false), BucketOrder.key(true)))
                        .subAggregation(new ReverseNestedAggregationBuilder("back_to_product"))));

        aggs.add(AggregationBuilders.nested("skus_nested_size", "skus")
                .subAggregation(AggregationBuilders.terms("skus_size")
                        .field("skus.size.raw")
                        .size(20)
                        .order(List.of(BucketOrder.count(false), BucketOrder.key(true)))
                        .subAggregation(new ReverseNestedAggregationBuilder("back_to_product"))));

        return aggs;
    }
}
