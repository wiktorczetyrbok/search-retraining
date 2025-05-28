package org.lucene.task.exc3;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcatenationAnalyzerTest {

    @Test
    public void testConcatenationFilterRemovesStopWordsAndConcatenates() throws Exception {
        Set<String> stopWords = Set.of("the", "is", "at", "which", "on");
        Analyzer analyzer = new ConcatenationAnalyzer(stopWords, " ");

        try (var ts = analyzer.tokenStream("field", new StringReader("the quick brown fox is at the zoo"))) {
            CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            if (ts.incrementToken()) {
                assertEquals("quick brown fox zoo", attr.toString());
            } else {
                throw new AssertionError("No token found");
            }
            ts.end();
        }
    }
}

