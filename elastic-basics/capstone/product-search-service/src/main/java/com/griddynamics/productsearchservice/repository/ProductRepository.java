package com.griddynamics.productsearchservice.repository;

import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;

public interface ProductRepository {
    ProductSearchResponse getAllProducts(ProductSearchRequest request);

    ProductSearchResponse getProductsByQuery(ProductSearchRequest request);

}
