package org.lucene.task.exc1;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

import java.io.IOException;

public class FileFinderLucene {

    public static void main(String[] args) throws IOException {
        Directory dir = new ByteBuffersDirectory();
        Analyzer analyzer = new KeywordAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        index(writer, "lucene/queryparser/docs/xml/img/plus.gif");
        index(writer, "lucene/queryparser/docs/xml/img/join.gif");
        index(writer, "lucene/queryparser/docs/xml/img/minusbottom.gif");

        writer.close();

        runQuery(dir, "lqdocspg");
        runQuery(dir, "lqd///gif");
        runQuery(dir, "minusbottom.gif");
    }

    private static void index(IndexWriter writer, String path) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("path", path, Field.Store.YES));
        writer.addDocument(doc);
    }

    private static void runQuery(Directory dir, String input) throws IOException {
        String regex = buildRegex(input);
        System.out.println("\nUser Input: " + input);
        System.out.println("Regex: " + regex);

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new WildcardQuery(new Term("path", regex));

            TopDocs results = searcher.search(query, 10);
            if (results.totalHits.value == 0) {
                System.out.println("No matches found.");
            } else {
                for (ScoreDoc sd : results.scoreDocs) {
                    Document doc = searcher.doc(sd.doc);
                    System.out.println("Match: " + doc.get("path"));
                }
            }
        }
    }

    private static String buildRegex(String input) {
        StringBuilder regex = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                regex.append(c).append("*");
            }
        }
        return regex.toString();
    }
}
