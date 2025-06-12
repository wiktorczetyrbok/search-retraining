package org.griddynamics.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;



@JsonInclude(Include.NON_NULL)
public record ProductSearchResponse(Long totalHits, List<ProductResponse> products) {
}
