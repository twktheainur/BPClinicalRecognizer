package org.sifrproject.recognizer.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.sifrproject.recognizer.FaironConceptRecognizer;
import org.sifrproject.recognizer.ConceptRecognizer;

import java.nio.file.Path;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class FaironRecognizerPooledObjectFactory extends BasePooledObjectFactory<ConceptRecognizer>  {

    private final Path dictionaryPath;

    public FaironRecognizerPooledObjectFactory(final Path dictionaryPath) {
        this.dictionaryPath = dictionaryPath;
    }

    @Override
    public ConceptRecognizer create() {
        return new FaironConceptRecognizer(dictionaryPath);
    }

    @Override
    public PooledObject<ConceptRecognizer> wrap(final ConceptRecognizer conceptRecognizer) {
        return new DefaultPooledObject<>(conceptRecognizer);
    }
}
