package com.griddynamics.productindexer.model;

import lombok.Data;


@Data
public class ProductSearchRequest {
    private String textQuery;
    private Integer size;
    public Integer getSize() {
        return (size != null && size > 0) ? size : 10;
    }
}
