package com.griddynamics.model;

import java.util.List;


public class BookHit {
    private String title;
    private List<String> authors;
    private String summary;
    private List<String> genres;

    public BookHit(String title, List<String> authors, String summary, List<String> genres) {
        this.title = title;
        this.authors = authors;
        this.summary = summary;
        this.genres = genres;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
