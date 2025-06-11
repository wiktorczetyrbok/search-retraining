package org.griddynamics.service;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.griddynamics.model.Product;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class IndexData {

    public static void indexData(List<Product> products) throws IOException {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(directory, config);

        for (Product product : products) {
            Document doc = new Document();
            doc.add(new StringField("ID", product.getId(), Field.Store.YES));
            doc.add(new TextField("Name", product.getName(), Field.Store.YES));
            doc.add(new TextField("Description", product.getDescription(), Field.Store.YES));
            doc.add(new StringField("Category", product.getCategory(), Field.Store.YES));
            doc.add(new DoublePoint("Price", product.getPrice()));
            writer.addDocument(doc);
        }

        writer.commit();
        writer.close();
        System.out.println(products.size() + " documents indexed.");
    }
}
