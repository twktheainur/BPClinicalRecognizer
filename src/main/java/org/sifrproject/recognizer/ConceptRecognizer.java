package org.sifrproject.recognizer;

import stormpot.Poolable;

import java.util.List;

public interface ConceptRecognizer extends Poolable {
    List<AnnotationToken> recognize(String inputText);
}
