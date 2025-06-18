package com.griddynamics.model;

import java.util.List;

public class BookSearchResult {
    private List<BookHit> hits;
    private List<GenreFacet> genreFacets;

    public BookSearchResult(List<BookHit> hits, List<GenreFacet> genreFacets) {
        this.hits = hits;
        this.genreFacets = genreFacets;
    }

    public List<BookHit> getHits() {
        return hits;
    }

    public void setHits(List<BookHit> hits) {
        this.hits = hits;
    }

    public List<GenreFacet> getGenreFacets() {
        return genreFacets;
    }

    public void setGenreFacets(List<GenreFacet> genreFacets) {
        this.genreFacets = genreFacets;
    }
}
