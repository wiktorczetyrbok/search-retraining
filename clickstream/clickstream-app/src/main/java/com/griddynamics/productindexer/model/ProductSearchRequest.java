package com.griddynamics.productindexer.model;

import lombok.Data;


@Data
public class ProductSearchRequest {
    private String textQuery;
    private Integer size;
    private boolean boostIncluded;
    private Integer page = 0;
    public Integer getSize() {
        return (size != null && size > 0) ? size : 10;
    }

    public boolean isBoostIncluded() {
        return boostIncluded;
    }

    public boolean isGetAllRequest() {
        return textQuery == null || textQuery.trim().isEmpty();
    }

}
