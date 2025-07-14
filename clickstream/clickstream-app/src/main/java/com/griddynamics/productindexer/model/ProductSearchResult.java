package com.griddynamics.productindexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResult {
    private int totalHits;
    private List<ProductSearchResponse> products;
    private Map<String, List<FacetBucket>> facets = new HashMap<>();
}
