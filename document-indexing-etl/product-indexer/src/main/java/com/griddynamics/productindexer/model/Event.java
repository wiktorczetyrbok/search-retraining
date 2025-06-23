package com.griddynamics.productindexer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Event {
    private int id;
    private String field;
    private Object before;
    private Object after;
}

