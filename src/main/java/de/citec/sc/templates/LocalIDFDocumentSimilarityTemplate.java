/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.exceptions.EmptyIndexException;
import de.citec.sc.helper.StanfordParser;
import de.citec.sc.helper.Tokenizer;
import de.citec.sc.similarity.database.FileDB;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.similarity.tfidf.TFIDF;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;

/**
 *
 * @author hterhors
 *
 * Feb 18, 2016
 */
public class LocalIDFDocumentSimilarityTemplate
        extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

    private static Logger log = LogManager.getFormatterLogger();

    /*
     * Read possible URIs...
     */
    private static Set<String> uris;

    /*
     * Link, TF-DocumentVector.
     */
    private static Map<String, Map<String, Integer>> documentVectors;

    private static StanfordParser lemmatizer;

    /**
     * Cache current document vector...
     */
    private static Map<String, Integer> currentInputTermFrequencyVector = null;
    private String currentDocumentName = null;

    /**
     * Cache vectors for each candidate.
     *
     */
    static Map<String, Map<String, Double>> candidateTFIDFVectors = null;
    private String currentEntityWord = null;

    static Map<String, Double> inputTFIDFVector = new HashMap<>();

    private static boolean isInitialized = false;

    private static FileDB db;

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void init(final String filesWithURIs, final String termfrequencyDBBin, final String termfrequencyFile)
            throws IOException, EmptyIndexException {

        if (!isInitialized) {

            lemmatizer = new StanfordParser();

            documentVectors = (Map<String, Map<String, Integer>>) restoreData(termfrequencyDBBin);

            if (documentVectors == null) {

                documentVectors = new HashMap<>();

                prepareUris(filesWithURIs);

                prepareDocumentVectors(termfrequencyDBBin, termfrequencyFile);

                writeData(termfrequencyDBBin, documentVectors);
            }
            isInitialized = true;
        }
    }

    private static void prepareUris(final String filesWithURIs) throws IOException {
        uris = Files.readAllLines(new File(filesWithURIs).toPath()).stream().distinct().collect(Collectors.toSet());
    }

    private static void prepareDocumentVectors(final String termfrequencyDBBin, final String termfrequencyFile)
            throws IOException, EmptyIndexException {
        /*
         * Load database...
         */
        db = new FileDB();
        db.loadIndicies(termfrequencyDBBin, termfrequencyFile, false);

        for (String uri : uris) {

            final Map<String, Integer> x = lineToVector(db.query(uri));
            if (x != null) {
                documentVectors.put(uri, x);
                log.info(uri + " with " + x.size() + " entries.");
            }
        }

        log.info("documentVectors = " + documentVectors.size());
    }

    public LocalIDFDocumentSimilarityTemplate() throws InitializationException {
        if (!isInitialized) {
            log.warn("LocalIDFDocumentSimilarityTemplate is NOT initialized correctly!");
            log.warn("Call LocalIDFDocumentSimilarityTemplate.init() for proper initlialization.");
            throw new InitializationException(
                    "LocalIDFDocumentSimilarityTemplate is NOT initialized correctly! Call LocalIDFDocumentSimilarityTemplate.init() for proper initlialization.");
        }
    }

    @Override
    public Set<SingleVariablePattern<Annotation>> generateFactorPatterns(State state) {
        Set<SingleVariablePattern<Annotation>> factors = new HashSet<>();
        for (Annotation a : state.getEntities()) {
            factors.add(new SingleVariablePattern<>(this, a));
        }
        log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
        return factors;
    }

    @Override
    public void computeFactor(Document instance, Factor<SingleVariablePattern<Annotation>> factor) {
        Annotation entity = factor.getFactorPattern().getVariable();
        log.debug("Compute %s factor for variable %s", LocalIDFDocumentSimilarityTemplate.class.getSimpleName(),
                entity);
        Vector featureVector = factor.getFeatureVector();

        String documentName = instance.getDocumentName();
        String documentText = instance.getDocumentContent();
        String entityWord = entity.getWord();
        String candidate = entity.getLink();
        Set<String> candidates = entity.getPossibleLinks();

        final boolean wasUpdated = updateTFVectorForDocument(documentName, documentText);

        updateCandidatesCache(wasUpdated, documentName, candidates);

        final Map<String, Double> candidateTFIDFVector = candidateTFIDFVectors.get(candidate);

        if (candidateTFIDFVector == null) {
            System.out.println("NULL HERE: " + candidate + "others : " + entity.getPossibleLinks());
        }

        double cosineSimilarity = getScoreForCandidate(candidateTFIDFVector);

        featureVector.set("Local_IDF_Document_Cosine_Similarity", cosineSimilarity);

        currentDocumentName = documentName;
        currentEntityWord = entityWord;
    }

    private boolean updateCandidatesCache(final boolean frorceUpdate, final String entityWord, Set<String> candidates) {

        if (frorceUpdate || currentEntityWord == null || !currentEntityWord.equals(entityWord)) {
            candidateTFIDFVectors = new HashMap<>();

            Map<String, Map<String, Integer>> localTermFrequencyVectorCorpus = returnLocalDocumentCorpus(
                    currentInputTermFrequencyVector, candidates);

            Map<String, Double> localIDFScores = calculateLocalIDFScores(currentInputTermFrequencyVector,
                    localTermFrequencyVectorCorpus);

            inputTFIDFVector = new HashMap<>();
            for (Entry<String, Integer> inputTF : currentInputTermFrequencyVector.entrySet()) {
                inputTFIDFVector.put(inputTF.getKey(), inputTF.getValue() * localIDFScores.get(inputTF.getKey()));
            }

            for (Entry<String, Map<String, Integer>> candidatesTFVecotrs : localTermFrequencyVectorCorpus.entrySet()) {

                candidateTFIDFVectors.put(candidatesTFVecotrs.getKey(), new HashMap<>());

                for (Entry<String, Integer> candidateTF : candidatesTFVecotrs.getValue().entrySet()) {
                    try {
                        
                        Map<String, Double> map =  candidateTFIDFVectors.get(candidatesTFVecotrs.getKey());
                        double idfScore = localIDFScores.get(candidateTF.getKey());
                        map.put(candidateTF.getKey(),candidateTF.getValue() * idfScore);
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("CandidateTF "+candidateTF);
                        System.out.println(localIDFScores);
                        System.out.println("Vectors"+candidatesTFVecotrs);
                        System.out.println(localIDFScores.get(candidateTF.getKey()));
                        System.out.println(candidateTFIDFVectors.get(candidatesTFVecotrs.getKey()));
                        System.exit(0);
                    }
                }

            }
            return true;
        }
        return false;
    }

    private static double getScoreForCandidate(final Map<String, Double> candidateTFIDFVector) {

        double sim = 0;
        if (inputTFIDFVector != null && candidateTFIDFVector != null) {
            try {
                sim = SimilarityMeasures.cosineDistance(inputTFIDFVector, candidateTFIDFVector);
            } catch (Exception e) {
                System.out.println("Vector null exception" + candidateTFIDFVector + " \\=> " + inputTFIDFVector);
                e.printStackTrace();
            }
        }

        if (Double.isNaN(sim)) {
            sim = 0;
        }

        return sim;

    }

    private static Map<String, Double> calculateLocalIDFScores(Map<String, Integer> inputTermFrequencyVector,
            Map<String, Map<String, Integer>> localDocumentVectorsCorpus) {

        Map<String, Set<String>> localCorpus = new HashMap<>();
        localCorpus.put("InputDocument", inputTermFrequencyVector.keySet());

        for (Entry<String, Map<String, Integer>> candidateVector : localDocumentVectorsCorpus.entrySet()) {
            localCorpus.put(candidateVector.getKey(), candidateVector.getValue().keySet());
        }

        return TFIDF.getIDFs(inputTermFrequencyVector.keySet(), localCorpus);
    }

    private static Map<String, Map<String, Integer>> returnLocalDocumentCorpus(
            Map<String, Integer> inputTermFrequencyVector, Set<String> candidates) {

        Map<String, Map<String, Integer>> localDocumentVectorsCorpus = new HashMap<>();

        for (String candidate : candidates) {

            Map<String, Integer> candidateTermFrequencyVector = documentVectors.get(candidate);

            if (candidateTermFrequencyVector != null) {
                candidateTermFrequencyVector.keySet().retainAll(inputTermFrequencyVector.keySet());
                localDocumentVectorsCorpus.put(candidate, candidateTermFrequencyVector);
            } else {
                localDocumentVectorsCorpus.put(candidate, new HashMap<String, Integer>());
            }

        }
        return localDocumentVectorsCorpus;
    }

    private boolean updateTFVectorForDocument(final String documentName, final String documentText) {

        if (currentDocumentName == null || !currentDocumentName.equals(documentName)) {
            final String tokenizedDocument = Tokenizer.bagOfWordsTokenizer(documentText, true, " ");
            List<String> document = lemmatizer.lemmatizeDocument(tokenizedDocument);
            currentInputTermFrequencyVector = TFIDF.getTFs(document);
            return true;
        }
        return false;
    }

    private static Map<String, Integer> lineToVector(final String line) {

        if (line == null) {
            return null;
        }

        final Map<String, Integer> vector = new HashMap<>();

        if (line.split("\t", 2).length == 2) {
            final String vectorData = line.split("\t", 2)[1];

            for (String dataPoint : vectorData.split("\t")) {
                final String[] data = dataPoint.split(" ");
                vector.put(data[0], Integer.parseInt(data[1]));
            }
        }

        return vector;
    }

    private static void writeData(final String filename, final Object data) {
        final long t = System.currentTimeMillis();

        FileOutputStream fileOut;
        try {
            System.out.println("Write to filesystem...");
            fileOut = new FileOutputStream(filename);
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();
            System.out.println(
                    "Serialized data is saved to : \"" + filename + "\" in " + (System.currentTimeMillis() - t));
        } catch (final Exception e) {
            System.out.println("Could not serialize data to: \"" + filename + "\": " + e.getMessage());
        }
    }

    private static Object restoreData(final String filename) {
        final long t = System.currentTimeMillis();
        Object data = null;
        FileInputStream fileIn;
        System.out.println("Restore data from : \"" + filename + "\" ...");
        try {
            fileIn = new FileInputStream(filename);
            ObjectInputStream in;
            in = new ObjectInputStream(fileIn);
            data = in.readObject();
            in.close();
            fileIn.close();
            System.out.println(
                    "Successfully restored data from : \"" + filename + "\" in " + (System.currentTimeMillis() - t));
        } catch (final Exception e) {
            System.out.println("Could not restored data from : \"" + filename + "\": " + e.getMessage());
            return null;
        }
        return data;
    }
}
