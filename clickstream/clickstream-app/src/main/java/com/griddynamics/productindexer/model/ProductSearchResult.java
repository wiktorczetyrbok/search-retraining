package com.griddynamics.productindexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductSearchResult {
    private int totalHits;
    private List<ProductSearchResponse> products;
}
