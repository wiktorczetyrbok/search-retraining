package com.griddynamics.productsearchservice.service;

import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;

public interface ProductService {
    ProductSearchResponse getServiceResponse(ProductSearchRequest request);
}
