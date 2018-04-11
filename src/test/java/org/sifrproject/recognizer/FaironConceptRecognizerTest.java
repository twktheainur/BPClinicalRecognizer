package org.sifrproject.recognizer;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class FaironConceptRecognizerTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(FaironConceptRecognizerTest.class);
    private static final String TEXT_1 = "Désamorçage cardio-circulatoire";
    private static final String TEXT_2 = "   QU ’ EST-CE QUE TYSABRI ET DANS QUEL CAS EST-IL UTILISE   TYSABRI est utilisé pour traiter la sclérose en plaques ( SEP ).   Les symptômes de SEP peuvent varier d ’ un patient à l ’ autre et il est possible que vous ne présentiez aucun des symptômes décrits ici , notamment : troubles de la marche , engourdissement du visage , des bras ou des jambes";
    private static final int CAS_START = 41;
    private static final int CAS_END = 43;

    private final ConceptRecognizer conceptRecognizer;

    public FaironConceptRecognizerTest() throws URISyntaxException {
        final Path dictionaryPath = Paths.get(FaironConceptRecognizerTest.class.getResource("/dictionary.txt").toURI());
        conceptRecognizer = new FaironConceptRecognizer(dictionaryPath);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testRecognize_simple() {
        //DéSAMORçARGE CARDIO-CIRCULATOIRE
        final List<AnnotationToken> tokenList = conceptRecognizer.recognize(TEXT_1, false);
        for (final AnnotationToken annotationToken: tokenList){
            assert annotationToken.getText().equals(TEXT_1);
            assert annotationToken.getStart() == 1;
            assert annotationToken.getEnd() == TEXT_1.length();
            logger.info(annotationToken.toString());
        }
    }

    public void testRecognize_medication_notice() {
        //DéSAMORçARGE CARDIO-CIRCULATOIRE
        final List<AnnotationToken> tokenList = conceptRecognizer.recognize(TEXT_2, false);
        for (final AnnotationToken annotationToken: tokenList){
            if(annotationToken.getText().equals("CAS")) {
                assert ((annotationToken.getStart() == CAS_START) && (annotationToken.getEnd() == CAS_END));
            } else if (annotationToken.getText().equals("utilise")){
                assert ((annotationToken.getStart() == CAS_START) && (annotationToken.getEnd() == CAS_END));
            }
        }
    }
}