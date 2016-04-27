/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.exceptions.EmptyIndexException;
import de.citec.sc.similarity.database.FileDB;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.similarity.tfidf.IDFProvider;
import de.citec.sc.variables.State;
import de.citec.sc.wikipedia.preprocess.WikipediaTFIDFVector;
import factors.Factor;
import factors.patterns.VariablePairPattern;
import learning.Vector;

/**
 *
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class CandidateSimilarityTemplate
		extends templates.AbstractTemplate<Document, State, VariablePairPattern<Annotation>> {

	private static Logger log = LogManager.getFormatterLogger();

	/**
	 * The number of wikipedia documents.
	 */
	static public double NUMBER_OF_WIKI_DOCUMENTS;

	private static boolean isInitialized = false;

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static void init(final String indexFile, final String tfidfFile, final String dfFile,
			final boolean storeIndexOnDrive) throws IOException {

		if (!isInitialized) {
			FileDB.loadIndicies(indexFile, tfidfFile, storeIndexOnDrive);
			IDFProvider.getIDF(dfFile);

			NUMBER_OF_WIKI_DOCUMENTS = WikipediaTFIDFVector.countLines(tfidfFile);
			isInitialized = true;
		}

	}

	public CandidateSimilarityTemplate() throws InitializationException {
		if (!isInitialized) {
			log.warn("DocumentSimilarityTemplate is NOT initialized correctly!");
			log.warn("Call DocumentSimilarityTemplate.init() for proper initlialization.");
			throw new InitializationException(
					"DocumentSimilarityTemplate is NOT initialized correctly! Call DocumentSimilarityTemplate.init() for proper initlialization.");
		}
	}

	@Override
	public Set<VariablePairPattern<Annotation>> generateFactorPatterns(State state) {
		Set<VariablePairPattern<Annotation>> factors = new HashSet<>();
		for (Annotation firstAnnotation : state.getEntities()) {
			for (Annotation secondAnnotation : state.getEntities()) {
				if (!firstAnnotation.equals(secondAnnotation)) {
					factors.add(new VariablePairPattern<>(this, firstAnnotation, secondAnnotation));
				}
			}
		}

		log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	public void computeFactor(Document instance, Factor<VariablePairPattern<Annotation>> factor) {
		Annotation entity1 = factor.getFactorPattern().getVariable1();
		Annotation entity2 = factor.getFactorPattern().getVariable2();
		log.debug("Compute %s factor for variable %s", CandidateSimilarityTemplate.class.getSimpleName(), entity1);
		log.debug("Compute %s factor for variable %s", CandidateSimilarityTemplate.class.getSimpleName(), entity2);
		Vector featureVector = factor.getFeatureVector();

		try {
			log.debug("Retrieve text for query link %s...", entity1.getLink());
			String queryResult1 = FileDB.query(entity1.getLink());
			String queryResult2 = FileDB.query(entity2.getLink());
			double cosineSimilarity = 0;
			if (queryResult1 != null && queryResult2 != null) {
				log.debug("Convert retrieved abstract to vector...");
				Map<String, Double> candidateVector1 = lineToVector(queryResult1);
				Map<String, Double> candidateVector2 = lineToVector(queryResult2);

				log.debug("Compute cosine similarity...");
				cosineSimilarity = SimilarityMeasures.cosineDistance(candidateVector1, candidateVector2);
				log.debug("Cosine similarity: %s", cosineSimilarity);

			} else {
				cosineSimilarity = 0;
			}
			featureVector.set("Candidate_Cosine_Similarity", cosineSimilarity);
		} catch (IOException | EmptyIndexException e) {
			System.exit(1);
			e.printStackTrace();
		}
	}

	private Map<String, Double> lineToVector(final String line) {

		final Map<String, Double> vector = new HashMap<String, Double>();

		if (line.split("\t", 2).length == 2) {
			final String vectorData = line.split("\t", 2)[1];

			for (String dataPoint : vectorData.split("\t")) {
				final String[] data = dataPoint.split(" ");
				vector.put(data[0], Double.parseDouble(data[1]));
			}
		}

		return vector;
	}

}
