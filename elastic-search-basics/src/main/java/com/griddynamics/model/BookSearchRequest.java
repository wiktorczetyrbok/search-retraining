package com.griddynamics.model;

import java.util.Optional;

public class BookSearchRequest {
    private String textQuery;
    private Optional<String> authorFilter;
    private Optional<String> genreFilter;

    public String getTextQuery() {
        return textQuery;
    }

    public void setTextQuery(String textQuery) {
        this.textQuery = textQuery;
    }

    public Optional<String> getAuthorFilter() {
        if (authorFilter == null || authorFilter.isEmpty()) {
            return Optional.empty();
        }

        return authorFilter;
    }

    public void setAuthorFilter(Optional<String> authorFilter) {
        this.authorFilter = authorFilter;
    }

    public Optional<String> getGenreFilter() {
        if (genreFilter == null || genreFilter.isEmpty()) {
            return Optional.empty();
        } else if (genreFilter.get().equals("")) {
            return Optional.empty();
        }

        return genreFilter;
    }


    public void setGenreFilter(Optional<String> genreFilter) {
        this.genreFilter = genreFilter;
    }
}
