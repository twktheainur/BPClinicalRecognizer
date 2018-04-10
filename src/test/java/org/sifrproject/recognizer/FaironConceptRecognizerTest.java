package org.sifrproject.recognizer;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class FaironConceptRecognizerTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(FaironConceptRecognizerTest.class);
    private static final String TEXT_1 = "Désamorçage cardio-circulatoire";

    private ConceptRecognizer conceptRecognizer;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Path dictionaryPath = Paths.get(FaironConceptRecognizerTest.class.getResource("/dictionary.txt").toURI());
        conceptRecognizer = new FaironConceptRecognizer(dictionaryPath);
    }

    public void testRecognize() {
        //DéSAMORçARGE CARDIO-CIRCULATOIRE
        final List<AnnotationToken> tokenList = conceptRecognizer.recognize(TEXT_1);
        for (final AnnotationToken annotationToken: tokenList){
            assert annotationToken.getText().equals(TEXT_1);
            assert annotationToken.getStart() == 1;
            assert annotationToken.getEnd() == TEXT_1.length();
            logger.info(annotationToken.toString());
        }
    }
}