package com.griddynamics.productindexer.controller;

import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;
import com.griddynamics.productindexer.model.SignalEvent;
import com.griddynamics.productindexer.repository.SignalAggregator;
import com.griddynamics.productindexer.repository.SignalIndexer;
import com.griddynamics.productindexer.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final SignalIndexer signalIndexer;
    private final SignalAggregator signalAggregator;

    @PostMapping("/recreateIndex")
    public void recreateIndex() {
        productService.recreateIndex();
    }

    @PostMapping("/search")
    public ResponseEntity<ProductSearchResult> searchProducts(@RequestBody ProductSearchRequest request) {
        return ResponseEntity.ok(productService.searchProducts(request));

    }

    @PostMapping("/click")
    public ResponseEntity<?> recordClick(@RequestBody Optional<SignalEvent> signalEvent) throws IOException {
        signalIndexer.indexSignals(signalEvent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signals/aggregate")
    public String aggregateSignals() {
        try {
            signalAggregator.aggregateAndUpdatePopularity();
            return "Signal aggregation completed successfully.";
        } catch (Exception e) {
            return "Signal aggregation failed: " + e.getMessage();
        }
    }

}
