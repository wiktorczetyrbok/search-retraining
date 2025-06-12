package org.griddynamics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Product {
    private String id;
    private String title;
    private String description;
    private List<String> brands;
    private List<String> categories;
    private Map<String, Attribute> attributes;
    private PriceInfo priceInfo;
    private String name;
    private Instant availableTime;
    private String uri;
    private List<Image> images;
}
