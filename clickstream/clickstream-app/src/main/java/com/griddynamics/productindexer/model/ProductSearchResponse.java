package com.griddynamics.productindexer.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProductSearchResponse {
    private Float score;
    private String id;
    private String title;
    private String description;
    private String price;
    private String currencyCode;
    private String category;
    private Map<String, String> attributes;
    private Integer popularity;
}
