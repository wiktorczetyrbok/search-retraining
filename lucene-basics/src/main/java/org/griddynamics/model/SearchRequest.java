package org.griddynamics.model;

import lombok.Data;

import java.util.Map;
import java.util.Optional;

@Data
public class SearchRequest {

    private String textQuery;
    private Optional<Map<String, String>> filters;
    private Integer size;

    public Optional<Map<String, String>> getFilters() {
        return (filters != null) ? filters : Optional.empty();
    }

    public Integer getSize() {
        return (size != null && size > 0) ? size : 10;
    }

}
