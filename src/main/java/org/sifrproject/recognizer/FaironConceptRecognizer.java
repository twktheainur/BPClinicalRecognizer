package org.sifrproject.recognizer;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[\\p{Punct}\\p{M}]");

    private final Path dictionaryPath;

    private final Map<String, Set<Long>> dictionaryUnigramIndex;
    private final Map<Long, Integer> conceptLengthIndex;

    private final Collection<String> stopList = new TreeSet<>();

    private final Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.FRENCH);
    private final SimpleTokenizer simpleTokenizer = SimpleTokenizer.INSTANCE;

    public FaironConceptRecognizer(final Path dictionaryPath){
        this.dictionaryPath = dictionaryPath;
        dictionaryUnigramIndex = new HashMap<>();
        conceptLengthIndex = new HashMap<>();
        loadStopWords();
        try {
            loadDictionary();
        } catch (final IOException e) {
            logger.error("FATAL - Failed to load dictionary: {}", e.getLocalizedMessage());
            System.exit(1);
        }

    }

    private void loadStopWords() {
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


    private void loadDictionary() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(dictionaryPath)) {
            while (reader.ready()) {
                final String line = reader.readLine();
                final String[] fields = line.split("\t");
                final String label = Normalizer.normalize(fields[1], Normalizer.Form.NFKD);
                final Long conceptId = Long.valueOf(fields[0]);

                final String normalizedLabel = normalizeString(label);

                final String[] tokens = simpleTokenizer.tokenize(normalizedLabel);

                int conceptTokenCount = 0;
                for (final String token : tokens) {
                    if (!stopList.contains(token)) {
                        final String tokenStem = stemmer
                                .stem(token)
                                .toString();
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
    }


    @Override
    public List<AnnotationToken> recognize(final String inputText) {
        final Collection<AnnotationToken> annotations = new ArrayList<>();
        final String normalizedInputText = normalizeString(inputText);
        final Span[] tokenSpans = simpleTokenizer.tokenizePos(normalizedInputText);
        int currentTokenSpanIndex = 0;
        while (currentTokenSpanIndex < tokenSpans.length) {
            final Span currentSpan = tokenSpans[currentTokenSpanIndex];
            final String token = tokenFromSpan(currentSpan, normalizedInputText);

            if (!stopList.contains(token)) {
                int matchCursor = 1;
                int stopCount = 0;
                Set<Long> concepts = getConceptsForStemFromIndex(stem(token));
                final int conceptStart = currentSpan.getStart();
                int conceptEnd = currentSpan.getEnd();
                while ((currentTokenSpanIndex + matchCursor) < tokenSpans.length) {
                    final Span nextSpan = tokenSpans[currentTokenSpanIndex + matchCursor];
                    final String nextToken = tokenFromSpan(nextSpan, inputText);
                    if (stopList.contains(nextToken)) {
                        stopCount++;
                    } else {
                        final String nextTokenStem = stem(nextToken);
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
                currentTokenSpanIndex += matchCursor;
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

    private String normalizeString(final CharSequence input) {
        return NORMALIZE_PATTERN
                .matcher(Normalizer.normalize(input, Normalizer.Form.NFKD))
                .replaceAll("")
                .toLowerCase();
    }

    private String stem(final CharSequence input) {
        return stemmer
                .stem(input)
                .toString();
    }

    private String tokenFromSpan(final Span span, final CharSequence text) {
        return text
                .subSequence(span.getStart(), span.getEnd())
                .toString();
    }

    private Collection<AnnotationToken> conceptsToAnnotationTokens(final Iterable<Long> concepts, final int start, final int end, final String text, final int tokenCardinality) {
        final Collection<AnnotationToken> annotations = new ArrayList<>();
        for (final Long conceptID : concepts) {
            annotations.add(AnnotationToken.create(start, end, text.substring(start, end), conceptID, tokenCardinality));
        }
        return annotations;
    }
}
