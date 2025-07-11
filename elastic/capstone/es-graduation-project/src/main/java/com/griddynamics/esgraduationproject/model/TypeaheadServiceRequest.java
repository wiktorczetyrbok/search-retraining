package com.griddynamics.esgraduationproject.model;

import lombok.Data;

@Data
public class TypeaheadServiceRequest {
    private Integer size;
    private String textQuery;
    private Boolean considerItemCountInSorting;

    public boolean isConsiderItemCountInSorting() {
        return Boolean.TRUE.equals(considerItemCountInSorting);
    }

    public boolean isGetAllRequest() {
        return textQuery == null;
    }
}
