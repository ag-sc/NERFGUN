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
import factors.AbstractFactor;
import factors.impl.SingleVariableFactor;
import learning.Vector;
import utility.VariableID;

/**
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class DocumentSimilarityTemplate extends templates.AbstractTemplate<State> {

	private static Logger log = LogManager.getFormatterLogger();
	private final StanfordLemmatizer lemmatizer;

	private Map<String, Double> currentDocumentVector = null;

	private String currentDocumentName = null;

	private String dfFile;
	/**
	 * The number of wikipedia documents.
	 */
	final public double NUMBER_OF_WIKI_DOCUMENTS;

	public DocumentSimilarityTemplate(final String indexFile, final String tfidfFile, final String dfFile,
			final boolean storeIndexOnDrive) throws IOException {

		FileDB.loadIndicies(indexFile, tfidfFile, storeIndexOnDrive);
		this.dfFile = dfFile;
		IDFProvider.getIDF(dfFile);
		lemmatizer = new StanfordLemmatizer();

		NUMBER_OF_WIKI_DOCUMENTS = WikipediaTFIDFVector.countLines(tfidfFile);

	}

	@Override
	protected Collection<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (VariableID entityID : state.getEntityIDs()) {
			factors.add(new SingleVariableFactor(this, entityID));
		}
		log.info("Generate %s factors for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	protected void computeFactor(State state, AbstractFactor absFactor) {
		if (absFactor instanceof SingleVariableFactor) {
			SingleVariableFactor factor = (SingleVariableFactor) absFactor;
			Annotation entity = state.getEntity(factor.entityID);
			log.info("Compute DocumentSimilarity factor for state %s and variable %s", state.getID(), entity);
			Vector featureVector = new Vector();

			try {
				log.debug("Retrieve text for query link %s...", entity.getWord());
				String queryResult = FileDB.query(entity.getWord());
				double cosineSimilarity = 0;
				if (queryResult != null) {
					log.debug("Convert retrieved abstract to vector...");
					Map<String, Double> candidateVector = lineToVector(queryResult);

					final String document = state.getDocument().getDocumentContent();

					log.debug("Convert document to vector...");
					Map<String, Double> currentDocumentVector = convertDocumentToVector(document,
							state.getDocument().getDocumentContent());

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
			factor.setFeatures(featureVector);
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
