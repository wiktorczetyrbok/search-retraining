package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;
import com.griddynamics.productindexer.repository.ProductIndexingRepository;
import com.griddynamics.productindexer.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductIndexingRepository productIndexingRepository;
    private final ProductSearchRepository productRepository;


    public void recreateIndex() {
        productIndexingRepository.recreateIndex();
    }

    @Value("${com.griddynamics.es.graduation.project.request.default.findByQuerySize}")
    private int defaultFindByQuerySize;
    @Value("${com.griddynamics.es.graduation.project.request.default.getAllSize}")
    private int defaultGetAllSize;
    @Value("${com.griddynamics.es.graduation.project.request.minQueryLength}")
    private int minQueryLength;


    public ProductSearchResult searchProducts(ProductSearchRequest request) {
        prepareServiceRequest(request);
        if (request.isGetAllRequest()) {
            return productRepository.getAllProducts(request);
        } else if (request.getTextQuery().length() < minQueryLength) {
            return new ProductSearchResult();
        } else {
            return productRepository.getProductsByQuery(request);
        }
    }

    private void prepareServiceRequest(ProductSearchRequest request) {
        if (request.getSize() == null || request.getSize() <= 0) {
            request.setSize(request.isGetAllRequest() ? defaultGetAllSize : defaultFindByQuerySize);
        }
    }
}
