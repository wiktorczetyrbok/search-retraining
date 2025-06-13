package org.griddynamics.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;


@ApplicationScoped
public class LuceneConfig {

    @Produces
    public Analyzer analyzer() {
        return new StandardAnalyzer();
    }

    @Produces
    public Directory produceDirectory() throws IOException {
        Path indexPath = Path.of("lucene-index");
        return FSDirectory.open(indexPath);
    }
}
