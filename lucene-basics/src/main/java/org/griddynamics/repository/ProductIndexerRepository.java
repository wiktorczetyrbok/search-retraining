package org.griddynamics.repository;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.griddynamics.mapper.DocumentMapper;
import org.griddynamics.model.Product;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class ProductIndexerRepository {

    @Inject
    private Directory produceDirectory;

    @Inject
    private Analyzer analyzer;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "lucene.products")
    private String productsPath;


    public Integer createIndex() {
        List<Product> products = readJson(productsPath, new TypeReference<>() {
        });

        try (IndexWriter writer = new IndexWriter(produceDirectory, new IndexWriterConfig(analyzer))) {
            writer.deleteAll();
            writer.commit();

            for (Product product : products) {
                writer.addDocument(DocumentMapper.map(product));
            }

            writer.commit();
            return products.size();

        } catch (IOException e) {
            throw new RuntimeException("Exception while creating an index", e);
        }
    }

    public void deleteProductById(String productId) {
        try (IndexWriter writer = new IndexWriter(produceDirectory, new IndexWriterConfig(analyzer))) {
            writer.deleteDocuments(new Term("id", productId));
            writer.commit();
        } catch (IOException e) {
            throw new RuntimeException("Exception while deleting a product", e);
        }
    }

    private <T> T readJson(String path, TypeReference<T> typeReference) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource file not found: " + path);
            }
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception while reading a file: " + path, e);
        }
    }
}
