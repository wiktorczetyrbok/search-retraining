package org.griddynamics.repository;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.griddynamics.config.LuceneProperties;
import org.griddynamics.model.Attribute;
import org.griddynamics.model.PriceInfo;
import org.griddynamics.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProductIndexerRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexerRepository.class);

    @Inject
    Directory produceDirectory;

    @Inject
    Analyzer analyzer;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    LuceneProperties luceneProperties;

    public void createIndex() {
        List<Product> products = readJsonResource(
                luceneProperties.productsPath(),
                new TypeReference<List<Product>>() {}
        );

        try (IndexWriter writer = new IndexWriter(produceDirectory, new IndexWriterConfig(analyzer))) {
            writer.deleteAll();
            writer.commit();

            for (Product product : products) {
                writer.addDocument(mapToDocument(product));
            }

            writer.commit();
            log.info("{} documents have been indexed", products.size());
        } catch (IOException e) {
            log.error("Cannot index documents", e);
            throw new RuntimeException(e);
        }
    }

    public void deleteProductById(String productId) {
        try (IndexWriter writer = new IndexWriter(produceDirectory, new IndexWriterConfig(analyzer))) {
            writer.deleteDocuments(new Term("id", productId));
            writer.commit();
            log.info("Product with id: {} has been deleted", productId);
        } catch (IOException e) {
            log.error("Cannot delete product with id: {}", productId, e);
            throw new RuntimeException(e);
        }
    }

    public void updateProduct(Product product) {
        try (IndexWriter writer = new IndexWriter(produceDirectory, new IndexWriterConfig(analyzer))) {
            Term term = new Term("id", product.getId());
            Document updatedDocument = mapToDocument(product);
            writer.updateDocument(term, updatedDocument);
            writer.commit();
            log.info("Product with id: {} has been updated", product.getId());
        } catch (IOException e) {
            log.error("Cannot update product with id: {}", product.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private Document mapToDocument(Product product) {
        Document document = new Document();
        document.add(new StringField("id", product.getId(), Field.Store.YES));
        document.add(new TextField("title", product.getTitle(), Field.Store.YES));
        document.add(new TextField("description", product.getDescription(), Field.Store.YES));
        document.add(new StringField("name", product.getName(), Field.Store.YES));
        document.add(new StringField("uri", product.getUri(), Field.Store.YES));
        document.add(new LongPoint("availableTime", product.getAvailableTime().toEpochMilli()));
        document.add(new StoredField("availableTime", product.getAvailableTime().toEpochMilli()));

        product.getBrands().forEach(brand ->
                document.add(new StringField("brand", brand, Field.Store.YES))
        );

        product.getCategories().forEach(category ->
                document.add(new TextField("category", category, Field.Store.YES))
        );

        for (Map.Entry<String, Attribute> entry : product.getAttributes().entrySet()) {
            String key = entry.getKey();
            entry.getValue().text().forEach(val ->
                    document.add(new StringField("attributes." + key, val, Field.Store.YES))
            );
        }

        PriceInfo priceInfo = product.getPriceInfo();
        document.add(new DoublePoint("price", priceInfo.price()));
        document.add(new StoredField("price", priceInfo.price()));
        document.add(new StringField("currencyCode", priceInfo.currencyCode(), Field.Store.YES));

        product.getImages().forEach(img ->
                document.add(new StoredField("image.uri", img.uri()))
        );

        return document;
    }

    private <T> T readJsonResource(String path, TypeReference<T> typeReference) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + path);
            }
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read resource file: " + path, e);
        }
    }
}
