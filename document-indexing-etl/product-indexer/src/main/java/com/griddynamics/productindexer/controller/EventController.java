package com.griddynamics.productindexer.controller;

import com.griddynamics.productindexer.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.griddynamics.productindexer.model.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {
    private final ProductService productService;

    @PostMapping("/triggerUpdates")
    public ResponseEntity<UpdateResult> updateProducts(@RequestBody List<Event> events) {
        return ResponseEntity.ok(productService.applyUpdateEvents(events));
    }
}
