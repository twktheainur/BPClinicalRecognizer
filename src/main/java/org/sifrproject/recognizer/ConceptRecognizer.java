package org.sifrproject.recognizer;

import java.util.List;

public interface ConceptRecognizer {
    List<AnnotationToken> recognize(String inputText);
}
