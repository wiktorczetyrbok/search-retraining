package com.griddynamics.esgraduationproject.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TypeaheadServiceResponse {
    private Long totalHits;
    private List<Map<String, Object>> typeaheads;
    private Map<String, Map<String, Map<String, Number>>> facets = new HashMap<>();
}
