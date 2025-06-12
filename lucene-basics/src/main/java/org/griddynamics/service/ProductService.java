package org.griddynamics.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.griddynamics.model.Product;
import org.griddynamics.model.ProductSearchRequest;
import org.griddynamics.model.ProductSearchResponse;
import org.griddynamics.repository.ProductIndexerRepository;
import org.griddynamics.repository.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@ApplicationScoped
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Inject
    ProductSearchRepository productSearchRepository;

    @Inject
    ProductIndexerRepository productIndexerRepository;

    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        if (request.textQuery() != null && !request.textQuery().isBlank()) {
            return productSearchRepository.getProductsByQuery(request);
        } else {
            return new ProductSearchResponse(0L, List.of());
        }
    }

    public void deleteProduct(String productId) {
        productIndexerRepository.deleteProductById(productId);
    }

    public void updateProduct(Product product) {
        productIndexerRepository.updateProduct(product);
    }

    public void createIndex() {
        productIndexerRepository.createIndex();
    }
}
