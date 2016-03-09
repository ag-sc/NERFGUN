/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.exceptions.EmptyIndexException;
import de.citec.sc.helper.StanfordLemmatizer;
import de.citec.sc.helper.Stopwords;
import de.citec.sc.helper.Tokenizer;
import de.citec.sc.similarity.database.FileDB;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.similarity.tfidf.IDFProvider;
import de.citec.sc.similarity.tfidf.TFIDF;
import de.citec.sc.variables.State;
import de.citec.sc.wikipedia.preprocess.WikipediaTFIDFVector;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;
import utility.VariableID;

/**
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class DocumentSimilarityTemplate
		extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

	private static Logger log = LogManager.getFormatterLogger();
	private static StanfordLemmatizer lemmatizer;

	private Map<String, Double> currentDocumentVector = null;

	private String currentDocumentName = null;

	private static String dfFile;
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
			DocumentSimilarityTemplate.dfFile = dfFile;
			IDFProvider.getIDF(dfFile);
			lemmatizer = new StanfordLemmatizer();

			NUMBER_OF_WIKI_DOCUMENTS = WikipediaTFIDFVector.countLines(tfidfFile);
			isInitialized = true;
		}

	}

	public DocumentSimilarityTemplate() throws InitializationException {
		if (!isInitialized) {
			log.warn("DocumentSimilarityTemplate is NOT initialized correctly!");
			log.warn("Call DocumentSimilarityTemplate.init() for proper initlialization.");
			throw new InitializationException(
					"DocumentSimilarityTemplate is NOT initialized correctly! Call DocumentSimilarityTemplate.init() for proper initlialization.");
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
		log.debug("Compute %s factor for variable %s", DocumentSimilarityTemplate.class.getSimpleName(), entity);
		Vector featureVector = factor.getFeatureVector();

		try {
			log.debug("Retrieve text for query link %s...", entity.getLink());
			String queryResult = FileDB.query(entity.getLink());
			double cosineSimilarity = 0;
			if (queryResult != null) {
				log.debug("Convert retrieved abstract to vector...");
				Map<String, Double> candidateVector = lineToVector(queryResult);

				final String document = instance.getDocumentContent();

				log.debug("Convert document to vector...");
				Map<String, Double> currentDocumentVector = convertDocumentToVector(document,
						instance.getDocumentContent());

				log.debug("Compute cosine similarity...");
				cosineSimilarity = SimilarityMeasures.cosineDistance(candidateVector, currentDocumentVector);
				log.debug("Cosine similarity: %s", cosineSimilarity);

			} else {
				cosineSimilarity = 0;
			}
			featureVector.set("Document_Cosine_Similarity", cosineSimilarity);
		} catch (IOException | EmptyIndexException e) {
			System.exit(1);
			e.printStackTrace();
		}
	}

	private Map<String, Double> convertDocumentToVector(final String document, final String documentName)
			throws IOException {

		if (currentDocumentName != null && currentDocumentName.equals(documentName))
			return currentDocumentVector;

		final List<String> preprocessedDocument = preprocessDocument(document);
		currentDocumentName = documentName;

		currentDocumentVector = TFIDF.getTFWikiIDF(preprocessedDocument, IDFProvider.getIDF(this.dfFile),
				NUMBER_OF_WIKI_DOCUMENTS);

		return currentDocumentVector;
	}

	private List<String> preprocessDocument(final String document) {

		final List<String> currentPreprocessedDocument;

		final String tokenizedDocument = Tokenizer.bagOfWordsTokenizer(document, Tokenizer.toLowercaseIfNotUpperCase,
				" ");

		currentPreprocessedDocument = lemmatizer.lemmatizeDocument(tokenizedDocument);

		currentPreprocessedDocument.removeAll(Stopwords.ENGLISH_STOP_WORDS);

		return currentPreprocessedDocument;
	}

	private Map<String, Double> lineToVector(final String line) {

		final Map<String, Double> vector = new HashMap<String, Double>();

		final String vectorData = line.split("\t", 2)[1];

		for (String dataPoint : vectorData.split("\t")) {
			final String[] data = dataPoint.split(" ");
			vector.put(data[0], Double.parseDouble(data[1]));
		}

		return vector;
	}

}
