package org.griddynamics.mapper;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;
import org.griddynamics.model.PriceInfo;
import org.griddynamics.model.Product;
import org.griddynamics.model.ProductResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class DocumentMapper {

    public static Document map(Product product) {
        Document doc = new Document();

        addStringField(doc, "id", product.getId());
        addTextField(doc, "title", product.getTitle());
        addTextField(doc, "description", product.getDescription());
        addStringField(doc, "name", product.getName());
        addStringField(doc, "uri", product.getUri());

        long timeMillis = product.getAvailableTime().toEpochMilli();
        doc.add(new LongPoint("availableTime", timeMillis));
        doc.add(new StoredField("availableTime", timeMillis));

        product.getBrands().forEach(brand -> addStringField(doc, "brand", brand));
        product.getCategories().forEach(category -> addTextField(doc, "category", category));
        product.getAttributes().forEach((key, attr) -> attr.text().forEach(value -> addStringField(doc, "attributes." + key, value)));
        product.getImages().forEach(image -> doc.add(new StoredField("image.uri", image.uri())));

        PriceInfo price = product.getPriceInfo();
        doc.add(new DoublePoint("price", price.price()));
        doc.add(new StoredField("price", price.price()));
        addStringField(doc, "currencyCode", price.currencyCode());
        return doc;
    }

    public static ProductResponse mapToResponse(ScoreDoc scoreDoc, StoredFields storedFields) {
        int docId = scoreDoc.doc;
        float score = scoreDoc.score;

        try {
            Document doc = storedFields.document(docId);
            Map<String, String> attributes = extractAttributes(doc);

            return ProductResponse.builder()
                    .score(score)
                    .id(doc.get("id"))
                    .title(doc.get("title"))
                    .description(doc.get("description"))
                    .price(doc.get("price"))
                    .currencyCode(doc.get("currencyCode"))
                    .category(doc.get("category"))
                    .attributes(attributes)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> extractAttributes(Document doc) {
        return doc.getFields().stream()
                .filter(f -> f.name().startsWith("attributes."))
                .collect(Collectors.groupingBy(
                        f -> f.name().substring("attributes.".length()),
                        Collectors.mapping(IndexableField::stringValue, Collectors.joining(","))));
    }

    private static void addStringField(Document doc, String name, String value) {
        if (value != null) {
            doc.add(new StringField(name, value, Field.Store.YES));
        }
    }

    private static void addTextField(Document doc, String name, String value) {
        if (value != null) {
            doc.add(new TextField(name, value, Field.Store.YES));
        }
    }
}
