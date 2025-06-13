package com.griddynamics.productsearchservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacetBucket {
    private String value;
    private Long count;
}

