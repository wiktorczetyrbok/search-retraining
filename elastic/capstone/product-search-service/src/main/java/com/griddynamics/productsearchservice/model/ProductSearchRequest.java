package com.griddynamics.productsearchservice.model;

import lombok.Data;

@Data
public class ProductSearchRequest {
    private String textQuery;
    private Integer size = 10;
    private Integer page = 0;

    public boolean isGetAllRequest() {
        return textQuery == null;
    }
}
