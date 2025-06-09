package com.griddynamics.productsearchservice.service;

import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;
import com.griddynamics.productsearchservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Value("${com.griddynamics.es.graduation.project.request.default.findByQuerySize}")
    private int defaultFindByQuerySize;
    @Value("${com.griddynamics.es.graduation.project.request.default.getAllSize}")
    private int defaultGetAllSize;
    @Value("${com.griddynamics.es.graduation.project.request.minQueryLength}")
    private int minQueryLength;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public ProductSearchResponse getServiceResponse(ProductSearchRequest request) {
        prepareServiceRequest(request);
        if (request.isGetAllRequest()) {
            return productRepository.getAllProducts(request);
        } else if (request.getTextQuery().length() < minQueryLength) {
            return new ProductSearchResponse();
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
