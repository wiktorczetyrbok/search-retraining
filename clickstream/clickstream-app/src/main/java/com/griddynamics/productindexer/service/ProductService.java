package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;
import com.griddynamics.productindexer.repository.ProductIndexingRepository;
import com.griddynamics.productindexer.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductIndexingRepository productIndexingRepository;
    private final ProductSearchRepository productSearchRepository;


    public void recreateIndex() {
        productIndexingRepository.recreateIndex();
    }

    public ProductSearchResult searchProducts(ProductSearchRequest request) {
        return productSearchRepository.searchProducts(request);
    }

}
