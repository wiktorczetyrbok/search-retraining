package org.griddynamics.model;

import java.util.Map;
import java.util.Optional;

public record ProductSearchRequest(String textQuery, Optional<Map<String, String>> filters, Integer size) {
    public ProductSearchRequest(String textQuery, Optional<Map<String, String>> filters, Integer size) {
        if (size == null) {
            size = 10;
        }

        this.textQuery = textQuery;
        this.filters = filters;
        this.size = size;
    }
}

