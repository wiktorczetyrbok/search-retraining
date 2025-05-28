
package org.lucene.task.exc2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

import java.io.IOException;
import java.util.*;

public class ProximitySearchLucene {

    public static void main(String[] args) throws IOException {
        Directory dir = new ByteBuffersDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        index(writer, "file1", "to be or not to be that is the question");
        index(writer, "file2", "make a long story short");
        index(writer, "file3", "see eye to eye");

        writer.close();

        List<QueryInput> queries = List.of(
                new QueryInput("to be not", 1),
                new QueryInput("to or to", 1),
                new QueryInput("to", 1),
                new QueryInput("long story short", 0),
                new QueryInput("long short", 0),
                new QueryInput("long short", 1),
                new QueryInput("story long", 1),
                new QueryInput("story long", 2)
        );

        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            for (QueryInput q : queries) {
                Query query = buildPhraseQuery("content", q.query, q.slop);
                TopDocs hits = searcher.search(query, 10);
                List<String> matchedFiles = new ArrayList<>();
                for (ScoreDoc scoreDoc : hits.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    matchedFiles.add(doc.get("filename"));
                }
                System.out.printf("\"%s\" %d - %s%n", q.query, q.slop, matchedFiles);
            }
        }
    }

    private static void index(IndexWriter writer, String fileName, String content) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("filename", fileName, Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.NO));
        writer.addDocument(doc);
    }

    private static Query buildPhraseQuery(String field, String text, int slop) {
        String[] terms = text.split("\\s+");
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.setSlop(slop);
        for (String term : terms) {
            builder.add(new Term(field, term));
        }
        return builder.build();
    }

    static class QueryInput {
        String query;
        int slop;

        public QueryInput(String query, int slop) {
            this.query = query;
            this.slop = slop;
        }
    }
}
