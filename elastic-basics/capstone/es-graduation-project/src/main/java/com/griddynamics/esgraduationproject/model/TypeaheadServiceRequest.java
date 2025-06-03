package com.griddynamics.esgraduationproject.model;

import lombok.Data;

@Data
public class TypeaheadServiceRequest {
    private Integer size;
    private String textQuery;

    public boolean isGetAllRequest() {
        return textQuery == null;
    }
}
