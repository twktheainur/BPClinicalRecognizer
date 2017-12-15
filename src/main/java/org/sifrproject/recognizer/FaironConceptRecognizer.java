package org.sifrproject.recognizer;


import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.sifrproject.stemming.FrenchClinicalStemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.SnowballStemmer;
import stormpot.Poolable;
import stormpot.Slot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FaironConceptRecognizer implements ConceptRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(FaironConceptRecognizer.class);
    private static final Pattern NORMALIZE_PUNCTUATION = Pattern.compile("['\\[\\]{}()]");
    private static final Pattern NORMALIZE_DIACRITICS = Pattern.compile("[\\p{M}]");

    private final Path dictionaryPath;

    private final Map<String, Set<Long>> dictionaryUnigramIndex;
    private final Map<Long, Integer> conceptLengthIndex;

    private final Collection<String> stopList = new TreeSet<>();

    private final Collection<String> terminationList = new TreeSet<>();

    private final SnowballStemmer stemmer = new FrenchClinicalStemmer();
    private final SimpleTokenizer simpleTokenizer = SimpleTokenizer.INSTANCE;

    private final Slot slot;

    public FaironConceptRecognizer(final Slot slot, final Path dictionaryPath) {
        this.dictionaryPath = dictionaryPath;
        dictionaryUnigramIndex = new HashMap<>();
        conceptLengthIndex = new HashMap<>();
        this.slot = slot;
        loadStopWords();
        loadTerminationTerms();
        try {
            loadDictionary();
        } catch (final IOException e) {
            logger.error("FATAL - Failed to load dictionary: {}", e.getLocalizedMessage());
            System.exit(1);
        }
    }

    public FaironConceptRecognizer(final Path dictionaryPath) {
        this(new DummySlot(), dictionaryPath);
    }

    private void loadStopWords() {
        logger.info("Loading stopwords from resources");
        try (InputStream stopStream = FaironConceptRecognizer.class.getResourceAsStream("/stopwords.fr.txt")) {
            try (BufferedReader stopReader = new BufferedReader(new InputStreamReader(stopStream))) {
                while (stopReader.ready()) {
                    stopList.add(stopReader.readLine());
                }
            }
        } catch (final IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    private void loadTerminationTerms() {
        logger.info("Loading termination terms from resources");
        try (InputStream terminationStream = FaironConceptRecognizer.class.getResourceAsStream("/termination_terms.fr.txt")) {
            try (BufferedReader terminationReader = new BufferedReader(new InputStreamReader(terminationStream))) {
                while (terminationReader.ready()) {
                    terminationList.add(terminationReader.readLine());
                }
            }
        } catch (final IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }


    private void loadDictionary() throws IOException {
        logger.info("Now loading dictionary...");
        try (BufferedReader reader = Files.newBufferedReader(dictionaryPath)) {
            while (reader.ready()) {
                final String line = reader.readLine();
                final String[] fields = line.split("\t");
                final String label = fields[1];
                final Long conceptId = Long.valueOf(fields[0]);

                final String normalizedLabel = normalizeString(label);

                final String[] tokens = simpleTokenizer.tokenize(normalizedLabel);

                int conceptTokenCount = 0;
                for (final String token : tokens) {
                    if (!stopList.contains(token)) {
                        final String tokenStem = stem(token);
                        if (!dictionaryUnigramIndex.containsKey(tokenStem)) {
                            dictionaryUnigramIndex.put(tokenStem, new HashSet<>());
                        }
                        dictionaryUnigramIndex
                                .get(tokenStem)
                                .add(conceptId);
                        conceptTokenCount++;
                    }
                }
                conceptLengthIndex.put(conceptId, conceptTokenCount);
            }
        }
        logger.info("Concept Recognizer ready!");
    }


    @Override
    public List<AnnotationToken> recognize(final String inputText) {
        Collection<AnnotationToken> annotations = Collections.emptyList();
        if ((inputText != null) && !inputText.isEmpty()) {
            logger.debug("Starting recognition");
            annotations = new ArrayList<>();
            final String normalizedInputText = normalizeString(inputText);
            final Span[] tokenSpans = simpleTokenizer.tokenizePos(normalizedInputText);
            int currentTokenSpanIndex = 0;
            while (currentTokenSpanIndex < tokenSpans.length) {
                final Span currentSpan = tokenSpans[currentTokenSpanIndex];
                final String token = tokenFromSpan(currentSpan, normalizedInputText);

                if (!stopList.contains(token) && !terminationList.contains(token)) {
                    //Double stemming ensures we come back to the most elementary root, ensure match between nouns and adjectives with
                    //the same root
                    Set<Long> concepts = getConceptsForStemFromIndex(stem(stem(token)));
                    final int conceptStart = currentSpan.getStart();
                    int conceptEnd = currentSpan.getEnd();
                    logger.debug("Matching from token {} in span [{},{}]", token, currentSpan.getStart(), currentSpan.getEnd());
                    int matchCursor = 1;
                    int stopCount = 0;
                    while ((currentTokenSpanIndex + matchCursor) < tokenSpans.length) {
                        final Span nextSpan = tokenSpans[currentTokenSpanIndex + matchCursor];
                        final String nextToken = tokenFromSpan(nextSpan, normalizedInputText);
                        if (stopList.contains(nextToken)) {
                            stopCount++;
                        } else if (terminationList.contains(nextToken)) {
                            break;
                        } else {
                            final String nextTokenStem = stem(stem(nextToken));
                            final Set<Long> nextConcepts = intersection(getConceptsForStemFromIndex(nextTokenStem), concepts);
                            if (nextConcepts.isEmpty()) {
                                break;
                            } else {
                                concepts = nextConcepts;
                                conceptEnd = nextSpan.getEnd();
                            }
                        }
                        matchCursor++;
                    }
                    annotations.addAll(conceptsToAnnotationTokens(concepts, conceptStart, conceptEnd, inputText, matchCursor - stopCount));
                }
                currentTokenSpanIndex += 1;
            }
        }
        return annotations
                .stream()
                .filter(a -> a.getTokenCardinality() == conceptLengthIndex.get(a.getConceptId()))
                .collect(Collectors.toList());
    }

    private Set<Long> getConceptsForStemFromIndex(final String stem) {
        final Set<Long> result = dictionaryUnigramIndex.get(stem);
        return (result == null) ? Collections.emptySet() : result;
    }

    private Set<Long> intersection(final Collection<Long> setA, final Set<Long> setB) {
        return setA
                .stream()
                .filter(setB::contains)
                .collect(Collectors.toSet());
    }

    private String normalizeCase(final String label) {
        return (stopList.contains(label.toLowerCase()) && isAllCaps(label)) ? label : label.toLowerCase();
    }

    private boolean isAllCaps(final CharSequence input) {
        int position = 0;
        while (Character.isUpperCase(input.charAt(position)) && (position<input.length())) {
            position++;
        }
        return position == input.length();
    }

    private String normalizeString(final CharSequence input) {
        final String withoutPunctuation = NORMALIZE_PUNCTUATION
                .matcher(input)
                .replaceAll(" ");
        return NORMALIZE_DIACRITICS
                .matcher(Normalizer.normalize(withoutPunctuation, Normalizer.Form.NFKD))
                .replaceAll("");
    }

    private String stem(final String input) {
        stemmer.setCurrent(input);
        return (stemmer.stem()) ? stemmer.getCurrent() : "";
    }

    private String tokenFromSpan(final Span span, final CharSequence text) {
        return text
                .subSequence(span.getStart(), span.getEnd())
                .toString();
    }

    private Collection<AnnotationToken> conceptsToAnnotationTokens(final Iterable<Long> concepts, final int start, final int end, final String text, final int tokenCardinality) {
        final String conceptText = text.substring(start, end);
        logger.trace("\tMatched \"{}\" in span [{},{}[", conceptText, start, end);
        final Collection<AnnotationToken> annotations = new ArrayList<>();
        for (final Long conceptID : concepts) {
            annotations.add(AnnotationToken.create(start, end, conceptText, conceptID, tokenCardinality));
        }
        return annotations;
    }

    @Override
    public void release() {
        slot.release(this);
    }

    private static class DummySlot implements Slot {
        @Override
        public void release(final Poolable poolable) {

        }

        @Override
        public void expire(final Poolable poolable) {

        }
    }
}
