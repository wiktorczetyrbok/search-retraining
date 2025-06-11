package org.griddynamics.model;

import lombok.Data;

@Data
public class Product {
    String id;
    String name;
    String description;
    String category;
    int price;
}
