package org.sifrproject.recognizer;


import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.sifrproject.recognizer.util.Sets;
import org.sifrproject.recognizer.util.Strings;
import org.sifrproject.recognizer.util.Tokens;
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
import java.util.*;
import java.util.stream.Collectors;

public class FaironConceptRecognizer implements ConceptRecognizer {

    private static final Logger logger = LoggerFactory.getLogger(FaironConceptRecognizer.class);

    private final InputStream dictionaryStream;

    private final Map<String, Set<Long>> dictionaryUnigramIndex;
    private final Map<Long, Integer> conceptLengthIndex;

    private final Collection<String> stopList;

    private final Collection<String> terminationList;

    private final SnowballStemmer stemmer = new FrenchClinicalStemmer();
    private final SimpleTokenizer simpleTokenizer = SimpleTokenizer.INSTANCE;

    private final Slot slot;

    public FaironConceptRecognizer(final Slot slot, final InputStream dictionaryStream) {
        this.dictionaryStream = dictionaryStream;
        dictionaryUnigramIndex = new HashMap<>();
        conceptLengthIndex = new HashMap<>();
        stopList = new TreeSet<>();
        terminationList = new TreeSet<>();
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

    public FaironConceptRecognizer(final InputStream dictionaryStream) {
        this(new DummySlot(), dictionaryStream);
    }

    /**
     * Load stop words from resources/stopwords.fr.txt
     */
    private void loadStopWords() {
        logger.info("Loading stopwords from resources");
        try (final InputStream stopStream = FaironConceptRecognizer.class
                .getClassLoader()
                .getResourceAsStream("stopwords.fr.txt")) {
            if (stopStream != null) {
                try (final BufferedReader stopReader = new BufferedReader(new InputStreamReader(stopStream))) {
                    while (stopReader.ready()) {
                        stopList.add(stopReader.readLine());
                    }
                }
            }
        } catch (final IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    /**
     * Loading termination terms from resources/termination_terms.fr.txt
     */
    private void loadTerminationTerms() {
        logger.info("Loading termination terms from resources");
        try (final InputStream terminationStream = FaironConceptRecognizer.class
                .getClassLoader()
                .getResourceAsStream("termination_terms.fr.txt")) {
            if (terminationStream != null) {
                try (final BufferedReader terminationReader = new BufferedReader(new InputStreamReader(terminationStream))) {
                    while (terminationReader.ready()) {
                        terminationList.add(terminationReader.readLine());
                    }
                }
            }
        } catch (final IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }


    /**
     * Load/index a dictionary in MGREP format
     * Format of each line: ID\tTERM
     *
     * @throws IOException is thrown if the file specified is not found
     */
    private void loadDictionary() throws IOException {
        logger.info("Now loading dictionary...");
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(dictionaryStream))) {
            //For each line of the file
            while (reader.ready()) {
                final String line = reader.readLine();
                // We split concept ids from labels
                final String[] fields = line.split("\t");
                final String label = fields[1];
                final Long conceptId = Long.valueOf(fields[0]);

                // We normalize the label (removing all puctuation)
                final String normalizedLabel = Strings.normalizeStringAndStripPuctuation(label);

                // We tokenize the label
                final String[] tokens = simpleTokenizer.tokenize(normalizedLabel);

                int conceptTokenCount = 0;
                //For each token
                for (final String token : tokens) {
                    //We skip words that belong to the stop list and words that contain non alphanumerical characters
                    if (!stopList.contains(token) && Strings.isAlphaNum(token)) {
                        final String tokenStem = stem(stem(token));
                        //We create the dictionary entry if it didn't exist before
                        if (!dictionaryUnigramIndex.containsKey(tokenStem)) {
                            dictionaryUnigramIndex.put(tokenStem, new HashSet<>());
                        }
                        //If it already existed, we add the concept id to the corresponding set
                        dictionaryUnigramIndex
                                .get(tokenStem)
                                .add(conceptId);
                        conceptTokenCount++;
                    }
                }
                //We keep track of the length in number of tokens for each concept
                conceptLengthIndex.put(conceptId, conceptTokenCount);
            }
        }
        logger.info("Concept Recognizer ready!");
    }


    @Override
    public List<AnnotationToken> recognize(final String inputText, final boolean longestOnly) {
        Collection<AnnotationToken> annotations = Collections.emptyList();
        if ((inputText != null) && !inputText.isEmpty()) {
            logger.debug("Starting recognition");
            annotations = new ArrayList<>();
            //We normalize the text (Remove punctuation and replace with whitespace)
            final String normalizedInputText = Strings.normalizeString(inputText);

            //We split the text into token spans (beging and end position from the start of the text)
            final Span[] tokenSpans = simpleTokenizer.tokenizePos(normalizedInputText);

            //We iterate over tokens one by one until we reach the end of the text
            int currentTokenSpanIndex = 0;
            while (currentTokenSpanIndex < tokenSpans.length) {
                //We get the current token span
                final Span currentSpan = tokenSpans[currentTokenSpanIndex];

                //We extract the string of the token from the text
                final String token = Tokens
                        .tokenFromSpan(currentSpan, normalizedInputText)
                        .trim();
                //If the word is a stoplist term or a termination term we skip it
                if (!stopList.contains(token) && !terminationList.contains(token)) {

                    //We get the concept ids matching the stem of the current token
                    //Double stemming ensures we come back to the most elementary root, ensure match between nouns and adjectives with
                    //the same root
                    Set<Long> concepts = getConceptsForStemFromIndex(stem(stem(token)));

                    //This is the start position of the first token of a matching sequence
                    final int conceptStart = currentSpan.getStart();
                    // For now we have matched a single terms, so currently the end position will be that of the
                    // current token
                    int conceptEnd = currentSpan.getEnd();

                    logger.debug("Matching from token {} in span [{},{}]", token, currentSpan.getStart(), currentSpan.getEnd());

                    //We will now try to find a maximal match starting from the current token
                    //We will iterate over subsequent words until we reach a termination term or until we can find
                    // no concept matches for a particular token
                    int matchCursor = 1;
                    int stopCount = 0;
                    while ((currentTokenSpanIndex + matchCursor) < tokenSpans.length) {
                        // We get the next token and position span
                        final Span nextSpan = tokenSpans[currentTokenSpanIndex + matchCursor];
                        final String nextToken = Tokens.tokenFromSpan(nextSpan, normalizedInputText);

                        //If the token is in the stop list we skip it and increment the count of skipped words
                        //We will need to subtract this from the total number of tokens for the concept
                        if (stopList.contains(nextToken)) {
                            stopCount++;
                            // If the token is a termination term, the matching process ends here
                        } else if (terminationList.contains(nextToken)) {
                            break;
                            //Otherwise we try to find a match for the token's stem in the dictionary index
                        } else {

                            //We stem the token text
                            final String nextTokenStem = stem(stem(nextToken));

                            //We try to find matching concepts and compute the intersection with previously identified concepts
                            final Set<Long> nextConcepts = Sets.intersection(getConceptsForStemFromIndex(nextTokenStem), concepts);

                            //If we fond none we stop the matching here
                            if (nextConcepts.isEmpty()) {
                                break;
                            } else {
                                //If we find a match, then we update the current end position to that of the
                                //currently matching token and update the intersected matched concept buffer
                                concepts = nextConcepts;
                                conceptEnd = nextSpan.getEnd();
                            }
                        }
                        //If we arrive here the current token has matched, we keep count of the current match length
                        matchCursor++;
                    }

                    //Once we get out of the loop we reconstruct the matches from the concepts remaining in the set
                    //after successive intersections, if concepts is empty there was no match and so
                    //Tokens.conceptsToAnnotationTokens will return an empty list otherwise we get a list of
                    //AnnotationToken objects instances that we add to the list of identified concepts
                    annotations.addAll(Tokens.conceptsToAnnotationTokens(concepts, conceptStart, conceptEnd, inputText, matchCursor - stopCount));
                }
                currentTokenSpanIndex += 1;
            }
        }

        //Here we filter the annotations to keep only those where the concept length matches the length of the
        //identified annotation
        return filterToMaximumLength(annotations);
    }

    private List<AnnotationToken> filterToMaximumLength(final Collection<AnnotationToken> annotations) {
        //For each AnnotationToken, filtering out those that do not match the length of the identified concepts
        return annotations
                .stream()
                .filter(a -> a.getTokenCardinality() == conceptLengthIndex.get(a.getConceptId()))
                .collect(Collectors.toList());
    }

    private Set<Long> getConceptsForStemFromIndex(final String stem) {
        final Set<Long> result = dictionaryUnigramIndex.get(stem);
        return (result == null) ? Collections.emptySet() : result;
    }


    private String stem(final String input) {
        stemmer.setCurrent(input);
        return (stemmer.stem()) ? stemmer.getCurrent() : "";
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
