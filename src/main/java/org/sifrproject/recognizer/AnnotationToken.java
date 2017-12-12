package org.sifrproject.recognizer;

public interface AnnotationToken {
    int getStart();
    int getEnd();
    int getTokenCardinality();
    String getText();
    Long getConceptId();

    static AnnotationToken create(final int start, final int end, final String text, final Long conceptId, final int tokenCardinality){
        return new AnnotationTokenImpl(start,end,text,conceptId, tokenCardinality);
    }
}
