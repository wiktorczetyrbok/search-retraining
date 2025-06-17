package com.griddynamics.controller;

import com.griddynamics.model.BookSearchRequest;
import com.griddynamics.model.BookSearchResponse;
import com.griddynamics.service.BookService;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

@Controller("/elastic-basics/books")
public class BookController {

    private final BookService bookService;

    @Inject
    public BookController(BookService articleService) {
        this.bookService = articleService;
    }

    @Post("/search")
    @Consumes("application/json")
    @Produces("application/json")
    public BookSearchResponse searchBooks(@Body BookSearchRequest request) {
        return bookService.searchBooks(request);
    }

    @Post("/index")
    @Status(HttpStatus.NO_CONTENT)
    public void createIndex() {
        bookService.createIndex();
    }
}
