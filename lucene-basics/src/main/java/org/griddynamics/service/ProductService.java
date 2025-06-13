package org.griddynamics.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.griddynamics.model.SearchRequest;
import org.griddynamics.model.SearchResponse;
import org.griddynamics.repository.ProductIndexerRepository;
import org.griddynamics.repository.ProductSearchRepository;

import java.util.List;


@ApplicationScoped
public class ProductService {

    @Inject
    ProductSearchRepository productSearchRepository;

    @Inject
    ProductIndexerRepository productIndexerRepository;

    public SearchResponse searchProducts(SearchRequest request) {
        if (request.getTextQuery() != null && !request.getTextQuery().isBlank()) {
            return productSearchRepository.getProductsByQuery(request);
        } else {
            return new SearchResponse(0L, List.of());
        }
    }

    public void deleteProduct(String productId) {
        productIndexerRepository.deleteProductById(productId);
    }

    public Integer createIndex() {
        return productIndexerRepository.createIndex();
    }
}
