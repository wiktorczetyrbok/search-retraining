package com.griddynamics.service;

import com.griddynamics.model.BookSearchRequest;
import com.griddynamics.model.BookSearchResult;
import com.griddynamics.repository.BookRepository;
import com.griddynamics.repository.BookSearchRepository;
import jakarta.inject.Singleton;

@Singleton
public class BookService {
    private final BookRepository bookRepository;
    private final BookSearchRepository bookSearchRepository;

    public BookService(BookRepository bookRepository, BookSearchRepository bookSearchRepository) {
        this.bookRepository = bookRepository;
        this.bookSearchRepository = bookSearchRepository;
    }

    public void createIndex() {
        bookRepository.recreateIndex();
    }

    public BookSearchResult searchBooks(BookSearchRequest request) {
        return bookSearchRepository.searchBooks(request);
    }

}
