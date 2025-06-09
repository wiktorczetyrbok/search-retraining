package com.griddynamics.productindexer;

import com.griddynamics.productindexer.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

import static java.util.Arrays.asList;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {
    private static final String RECREATE_INDEX_ARG = "recreateIndex";

    @Autowired
    ProductService productService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... strings) {
        List<String> args = asList(strings);
        boolean needRecreateIndex = args.contains(RECREATE_INDEX_ARG);
        if (needRecreateIndex) {
            productService.recreateIndex();
        }
    }
}
