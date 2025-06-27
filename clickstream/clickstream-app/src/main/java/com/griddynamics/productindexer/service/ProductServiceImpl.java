package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;
import com.griddynamics.productindexer.repository.ProductRepository;
import com.griddynamics.productindexer.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;


    @Override
    public void recreateIndex() {
        productRepository.recreateIndex();
    }

    @Override
    public ProductSearchResult searchProducts(ProductSearchRequest request) {
        return productSearchRepository.searchProducts(request);
    }

}
