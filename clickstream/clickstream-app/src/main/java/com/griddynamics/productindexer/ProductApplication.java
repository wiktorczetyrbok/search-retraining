package com.griddynamics.productindexer;

import com.griddynamics.productindexer.repository.SignalAggregator;
import com.griddynamics.productindexer.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class ProductApplication implements CommandLineRunner {

    @Autowired
    ProductService productService;
    @Autowired
    SignalAggregator signalAggregator;

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }

    @Override
    public void run(String... strings) throws IOException {
//       productService.recreateIndex();
//
//        signalAggregator.aggregateAndUpdatePopularity();
    }
}
