package com.griddynamics.model;
import lombok.Data;

import java.util.List;

@Data
public class Book {
    private String title;
    private String summary;
    private List<String> authors;
    private List<String> genres;
    private String publisher;
    private int publishYear;
}
