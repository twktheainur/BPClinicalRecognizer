package org.sifrproject.recognizer;

public class AnnotationTokenImpl implements AnnotationToken {

    private final int start;
    private final int end;
    private final String text;
    private final Long conceptId;
    private final int tokenCardinality;

    @Override
    public int getTokenCardinality() {
        return tokenCardinality;
    }

    AnnotationTokenImpl(final int start, final int end, final String text, final Long conceptId, final int tokenCardinality) {
        this.start = start;
        this.end = end;
        this.text = text;
        this.conceptId = conceptId;
        this.tokenCardinality = tokenCardinality;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Long getConceptId() {
        return conceptId;
    }

    @Override
    public String toString() {
        return String.format("%d\t%d\t%d\t%s", conceptId, start, end, text);
    }
}
