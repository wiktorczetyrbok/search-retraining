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

        try (var ts = analyzer.tokenStream("field", new StringReader("the test case for the task is at the testing suite which is on the disk"))) {
            CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            if (ts.incrementToken()) {
                assertEquals("test case for task testing suite disk", attr.toString());
            } else {
                throw new AssertionError("No token found");
            }
            ts.end();
        }
    }
}

