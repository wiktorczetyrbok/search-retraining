package org.lucene.task.exc3;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import java.util.ArrayList;
import java.util.Set;

public class ConcatenationAnalyzer extends Analyzer {
    private final Set<String> stopWords;
    private final String delimiter;

    public ConcatenationAnalyzer(Set<String> stopWords, String delimiter) {
        this.stopWords = stopWords;
        this.delimiter = delimiter;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream tokenStream = new StopFilter(source, StopFilter.makeStopSet(new ArrayList<>(stopWords)));
        tokenStream = new ConcatenationTokenFilter(tokenStream, delimiter);
        return new TokenStreamComponents(source, tokenStream);
    }
}
