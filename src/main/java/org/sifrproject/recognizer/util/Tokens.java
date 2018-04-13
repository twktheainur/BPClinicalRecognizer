package org.sifrproject.recognizer.util;

import opennlp.tools.util.Span;
import org.sifrproject.recognizer.AnnotationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("all")
public enum Tokens {
    ;

    private static final Logger logger = LoggerFactory.getLogger(Tokens.class);

    public static String tokenFromSpan(final Span span, final CharSequence text) {
        return text
                .subSequence(span.getStart(), span.getEnd())
                .toString();
    }

    public static Collection<AnnotationToken> conceptsToAnnotationTokens(final Iterable<Long> concepts, final int start, final int end, final String text, final int tokenCardinality) {
        final int adjustedEnd = Math.min(end,text.length());
        final String conceptText = text.substring(start, adjustedEnd);
        logger.trace("\tMatched \"{}\" in span [{},{}[", conceptText, start +1, end);
        final Collection<AnnotationToken> annotations = new ArrayList<>();
        for (final Long conceptID : concepts) {
            annotations.add(AnnotationToken.create(start+1, end, conceptText, conceptID, tokenCardinality));
        }
        return annotations;
    }

}
