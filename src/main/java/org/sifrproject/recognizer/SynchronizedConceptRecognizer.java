package org.sifrproject.recognizer;

import java.util.List;

public class SynchronizedConceptRecognizer implements ConceptRecognizer {

    private final ConceptRecognizer conceptRecognizer;

    public SynchronizedConceptRecognizer(final ConceptRecognizer conceptRecognizer) {
        this.conceptRecognizer = conceptRecognizer;
    }

    @Override
    public synchronized List<AnnotationToken> recognize(final String inputText) {
        return conceptRecognizer.recognize(inputText);
    }

    @Override
    public synchronized void release() {
        conceptRecognizer.release();
    }
}
