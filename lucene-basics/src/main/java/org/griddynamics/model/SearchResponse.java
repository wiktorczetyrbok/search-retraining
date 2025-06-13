package org.griddynamics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.List;


@JsonInclude(Include.NON_NULL)
public record SearchResponse(Long totalHits, List<ProductResponse> products) {
}
