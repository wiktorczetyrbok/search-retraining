package org.lucene.task.exc3;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public final class ConcatenationTokenFilter extends TokenFilter {
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final StringBuilder buffer = new StringBuilder();
    private final String delimiter;

    public ConcatenationTokenFilter(TokenStream input, String delimiter) {
        super(input);
        this.delimiter = delimiter;
    }

    @Override
    public boolean incrementToken() throws IOException {
        buffer.setLength(0);
        boolean hasToken = false;

        while (input.incrementToken()) {
            if (hasToken) buffer.append(delimiter);
            buffer.append(termAttr.toString());
            hasToken = true;
        }

        if (hasToken) {
            clearAttributes();
            termAttr.append(buffer.toString());
            return true;
        }

        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }
}
