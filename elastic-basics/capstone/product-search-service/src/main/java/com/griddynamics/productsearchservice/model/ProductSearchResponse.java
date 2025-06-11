package com.griddynamics.productsearchservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProductSearchResponse {
    private Long totalHits;
    private List<Map<String, Object>> products = new ArrayList<>();
    private Map<String, List<FacetBucket>> facets = new HashMap<>();
}


