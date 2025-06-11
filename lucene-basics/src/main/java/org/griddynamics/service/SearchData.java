package org.griddynamics.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;


public class SearchData {

    public static void searchData(String queryStr, int topN) throws Exception {
        Directory directory = FSDirectory.open(Paths.get("index"));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        QueryParser parser = new QueryParser("Description", new StandardAnalyzer());
        Query query = parser.parse(queryStr);

        TopDocs results = searcher.search(query, topN);
        System.out.println("Total hits: " + results.totalHits);
        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String title = doc.get("Title");
            String description = doc.get("Description");
            System.out.println("Title: " + title);
            System.out.println("Description: " + description);
            System.out.println("Score: " + scoreDoc.score);
        }

        reader.close();
    }
}
