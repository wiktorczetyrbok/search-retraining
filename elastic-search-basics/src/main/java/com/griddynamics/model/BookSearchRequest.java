package com.griddynamics.model;

import lombok.Data;

import java.util.Optional;

@Data
public class BookSearchRequest {
    private String textQuery;
    private Optional<String> authorFilter;
    private Optional<String> genreFilter;
}
