package com.griddynamics.productindexer.service;

import com.griddynamics.productindexer.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository typeaheadRepository;


    @Override
    public void recreateIndex() {
        typeaheadRepository.recreateIndex();
    }
}
