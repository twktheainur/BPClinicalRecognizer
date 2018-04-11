package org.sifrproject.recognizer.pool;

import org.sifrproject.recognizer.ConceptRecognizer;
import org.sifrproject.recognizer.FaironConceptRecognizer;
import stormpot.Allocator;
import stormpot.Slot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class FaironRecognizerAllocator implements Allocator<ConceptRecognizer> {

    private final Path dictionaryPath;

    public FaironRecognizerAllocator(final Path dictionaryPath) {
        this.dictionaryPath = dictionaryPath;
    }

    @Override
    public ConceptRecognizer allocate(final Slot slot) throws FileNotFoundException {
        return new FaironConceptRecognizer(slot,new FileInputStream(dictionaryPath.toFile()));
    }

    @Override
    public void deallocate(final ConceptRecognizer conceptRecognizer) {

    }
}
