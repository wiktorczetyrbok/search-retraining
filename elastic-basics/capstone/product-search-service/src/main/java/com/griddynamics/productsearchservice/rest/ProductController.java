package com.griddynamics.productsearchservice.rest;

import com.griddynamics.productsearchservice.model.ProductSearchRequest;
import com.griddynamics.productsearchservice.model.ProductSearchResponse;
import com.griddynamics.productsearchservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProductSearchResponse getSearchServiceResponse(@RequestBody ProductSearchRequest request) {
        return productService.getServiceResponse(request);
    }
}
