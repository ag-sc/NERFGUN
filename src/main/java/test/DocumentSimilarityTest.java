/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.evaluator.PRF1;
import de.citec.sc.exceptions.EmptyIndexException;
import de.citec.sc.helper.StanfordLemmatizer;
import de.citec.sc.helper.Stopwords;
import de.citec.sc.helper.Tokenizer;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnLucene;
import de.citec.sc.query.Instance;
import de.citec.sc.similarity.database.FileDB;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.similarity.tfidf.IDFProvider;
import de.citec.sc.similarity.tfidf.TFIDF;
import de.citec.sc.wikipedia.preprocess.WikipediaTFIDFVector;

public class DocumentSimilarityTest {

	private static StanfordLemmatizer lemmatizer;

	/**
	 * The number of wikipedia documents.
	 */
	public static double NUMBER_OF_WIKI_DOCUMENTS;
	public static String indexFile = "tfidfIndexDatabase.bin";
	public static String tfidfFile = "en_wiki_large_abstracts.tfidf";
	public static String dfFile = "en_wiki_large_abstracts.docfrequency";

	public static void main(String[] args) throws IOException, EmptyIndexException {

		boolean storeIndexOnDrive = true;

		FileDB.loadIndicies(indexFile, tfidfFile, storeIndexOnDrive);

		IDFProvider.getIDF(dfFile);

		lemmatizer = new StanfordLemmatizer();

		NUMBER_OF_WIKI_DOCUMENTS = WikipediaTFIDFVector.countLines(tfidfFile);

		CandidateRetriever indexSearch = new CandidateRetrieverOnLucene(false, "dbpediaIndex", "anchorIndex");

		CorpusLoader loader = new CorpusLoader();
		DefaultCorpus c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLL);

		// read file into stream, try-with-resources
		Evaluator evaluator = new Evaluator();

		Map<String, Set<Annotation>> gold = new HashMap<String, Set<Annotation>>();
		Map<String, Set<Annotation>> results = new HashMap<String, Set<Annotation>>();

		int counter = 0;
		final int max = c.getDocuments().size();

		for (Document d : c.getDocuments()) {
			System.out.print((counter++) + "/" + max + ": ");
			final String document = d.getDocumentContent();

			Map<String, Double> currentDocumentVector = convertDocumentToVector(document);

			List<Annotation> annotations = new ArrayList<Annotation>();
			gold.put(d.getDocumentName(), new HashSet<Annotation>());
			results.put(d.getDocumentName(), new HashSet<Annotation>());
			for (Annotation a : d.getGoldResult()) {
				gold.get(d.getDocumentName()).add(a);

				// String bestEntity = getFirst(indexSearch, a.getWord());

				// String bestEntity = getBestByStringSimilarity(indexSearch,
				// a.getWord());
				//
				String bestEntity = getBestByCosineSimilarity(indexSearch, currentDocumentVector, a.getWord());

				// String bestEntity = getBestBothSim(indexSearch, a.getWord(),
				// currentDocumentVector);

				// System.out.println(bestEntity);
				// System.out.println(a.getWord());
				if (bestEntity == null) {
					continue;
				}

				Annotation a1 = a.clone();
				a1.setLink(bestEntity.replace("http://dbpedia.org/resource/", "http://en.wikipedia.org/wiki/"));
				annotations.add(a1);
				results.get(d.getDocumentName()).add(a1);
			}
			d.setAnnotations(annotations);
			// System.out.println(d);
			System.out.println(evaluator.evaluate(d));
		}
		// evaluator.evaluateAll(c.getDocuments()).entrySet().forEach(System.out::println);

		PRF1.calculate(gold, results);

	}

	private static String getBestBothSim(CandidateRetriever indexSearch, String word,
			Map<String, Double> currentDocumentVector) throws IOException, EmptyIndexException {
		// Micro precision = 0.544
		// Micro recall = 0.502
		// Micro F1 = 0.522
		//
		// Macro precision = 0.541
		// Macro recall = 0.509
		// Macro F1 = 0.523
		//
		double maxScore = 0;

		String bestEntity = null;

		List<Instance> candidates = indexSearch.getAllResources(word, 100);
		int maxDist = 40;

		double candRank = 0;
		int count = 1;
		for (Instance inst : candidates) {
			final String candidate = inst.getUri().replace("http://dbpedia.org/resource/", "");
			candRank = Math.pow(1 + (1 - (count / candidates.size())), 4);
			int levenDist = SimilarityMeasures.levenshteinDistance(candidate, word);

			// maxDist = Math.max(maxDist, levenDist);

			double cosineSimilarity = 0;

			final String datapoint = FileDB.query("<" + candidate + ">");

			/*
			 * This should not happen.
			 */
			if (datapoint != null) {

				Map<String, Double> candidateVector = lineToVector(datapoint);

				cosineSimilarity = SimilarityMeasures.cosineDistance(candidateVector, currentDocumentVector);
			}

			final double score = (maxDist - levenDist) * cosineSimilarity * candRank;

			if (maxScore < score) {
				bestEntity = candidate;
				maxScore = score;
			}

		}
		return bestEntity;
	}

	private static String getFirst(CandidateRetriever indexSearch, String word) {
		// Micro precision = 0.703
		// Micro recall = 0.701
		// Micro F1 = 0.702
		//
		// Macro precision = 0.726
		// Macro recall = 0.724
		// Macro F1 = 0.725
		List<Instance> candidates = indexSearch.getAllResources(word, 100);

		for (Instance candidate : candidates) {
			return candidate.getUri().replace("http://dbpedia.org/resource/", "");
		}
		return null;
	}

	private static String getBestByStringSimilarity(CandidateRetriever indexSearch, String word) {

		// Micro-average Precision=0.548
		// Micro-average Recall=0.546
		// F1 Micro-average=0.547
		// Macro-average Precision=0.573
		// Macro-average Recall=0.571
		// F1 Macro-average=0.572
		int maxDist = Integer.MAX_VALUE;

		String bestEntity = null;

		List<Instance> candidates = indexSearch.getAllResources(word, 100);

		for (Instance instance : candidates) {

			String candidate = instance.getUri().replace("http://dbpedia.org/resource/", "");

			int levenDist = SimilarityMeasures.levenshteinDistance(candidate, word);
			if (maxDist > levenDist) {
				bestEntity = candidate;
				maxDist = levenDist;
			}

		}
		return bestEntity;
	}

	private static String getBestByCosineSimilarity(CandidateRetriever indexSearch,
			Map<String, Double> currentDocumentVector, String word) throws IOException, EmptyIndexException {
		// Micro-average Precision=0.349
		// Micro-average Recall=0.327
		// F1 Micro-average=0.337
		// Macro-average Precision=0.331
		// Macro-average Recall=0.315
		// F1 Macro-average=0.323

		double maxSim = 0;

		String bestEntity = null;

		List<Instance> candidates = indexSearch.getAllResources(word, 100);

		for (Instance i1 : candidates) {
			String candidate = i1.getUri().replace("http://dbpedia.org/resource/", "");
			final String datapoint = FileDB.query(candidate);

			/*
			 * This should not happen.
			 */
			if (datapoint == null) {
				continue;
			}

			Map<String, Double> candidateVector = lineToVector(datapoint);

			Double cosineSimilarity = SimilarityMeasures.cosineDistance(candidateVector, currentDocumentVector);

			if (maxSim < cosineSimilarity) {
				bestEntity = candidate;
				maxSim = cosineSimilarity;
			}

		}
		return bestEntity;
	}

	private static Map<String, Double> convertDocumentToVector(final String document) throws IOException {
		Map<String, Double> currentDocumentVector;
		final List<String> preprocessedDocument = preprocessDocument(document);

		currentDocumentVector = TFIDF.getTFWikiIDF(preprocessedDocument, IDFProvider.getIDF(dfFile),
				NUMBER_OF_WIKI_DOCUMENTS);

		return currentDocumentVector;
	}

	private static List<String> preprocessDocument(final String document) {

		final List<String> currentPreprocessedDocument;

		final String tokenizedDocument = Tokenizer.bagOfWordsTokenizer(document, Tokenizer.toLowercaseIfNotUpperCase,
				" ");

		currentPreprocessedDocument = lemmatizer.lemmatizeDocument(tokenizedDocument);

		currentPreprocessedDocument.removeAll(Stopwords.ENGLISH_STOP_WORDS);

		return currentPreprocessedDocument;
	}

	private static Map<String, Double> lineToVector(final String line) {

		final Map<String, Double> vector = new HashMap<String, Double>();

		final String vectorData = line.split("\t", 2)[1];

		for (String dataPoint : vectorData.split("\t")) {
			final String[] data = dataPoint.split(" ");
			vector.put(data[0], Double.parseDouble(data[1]));
		}

		return vector;
	}
}
