package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.model.ProductSearchRequest;
import com.griddynamics.productindexer.model.ProductSearchResult;

public interface ProductService {

    void recreateIndex();

    ProductSearchResult searchProducts(ProductSearchRequest request);

}
