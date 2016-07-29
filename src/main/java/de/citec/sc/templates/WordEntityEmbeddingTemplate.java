/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.FeatureUtils;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;
import templates.AbstractTemplate;

/**
 *
 * Computes factors between an assigned uri and the respective surface form
 * based on similarities of wikipedia word-entity embedding vectors.
 *
 * @author sjebbara
 *
 *         Jul 27, 2016
 */
public class WordEntityEmbeddingTemplate extends AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

	/*
	 * Handmade
	 */
	final private static int NUMBER_OF_BINS = 100;

	private static double[] bins = FeatureUtils.initializeBins(NUMBER_OF_BINS);

	private static Logger log = LogManager.getFormatterLogger();
	private static String NAMESPACE = "http://en.wikipedia.org/wiki/";
	private static boolean isInitialized = false;

	private static final Pattern TOKENIZATION_PATTERN = Pattern.compile("\\w+");

	private boolean useBins = false;
	private static WordVectors vectors = null;

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static void init(final String embeddingFilename) throws IOException {
		if (!isInitialized) {
			File gModel = new File(embeddingFilename);
			log.info("loading embeddings...");
			vectors = WordVectorSerializer.loadGoogleModel(gModel, true);
			log.info("Done, loading embeddings");
			isInitialized = true;
		}
	}

	public WordEntityEmbeddingTemplate(boolean b) throws InitializationException {
		if (!isInitialized) {
			log.warn("WordEntityEmbeddingTemplate is NOT initialized correctly!");
			log.warn("Call WordEntityEmbeddingTemplate.init() for proper initlialization.");
			throw new InitializationException(
					"WordEntityEmbeddingTemplate is NOT initialized correctly! Call WordEntityEmbeddingTemplate.init() for proper initlialization.");
		}

		this.useBins = b;
	}

	@Override
	public Set<SingleVariablePattern<Annotation>> generateFactorPatterns(State state) {
		Set<SingleVariablePattern<Annotation>> factors = new HashSet<>();
		for (Annotation annotation : state.getEntities()) {
			factors.add(new SingleVariablePattern<>(this, annotation));
		}

		log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	public void computeFactor(Document instance, Factor<SingleVariablePattern<Annotation>> factor) {
		Annotation annotation = factor.getFactorPattern().getVariable();
		log.debug("Compute %s factor for variable %s", WordEntityEmbeddingTemplate.class.getSimpleName(), annotation);

		Vector featureVector = factor.getFeatureVector();

		double score = 0;

		final double[] urlVector = getVectorForURI(annotation.getLink());
		final double[] surfaceFormVector = getVectorForSurfaceForm(annotation.getWord());

		if (urlVector != null && surfaceFormVector != null) {
			score = cosineSimilarity(urlVector, surfaceFormVector);
		} else {
			score = 0;
		}
		featureVector.set("WordEntityEmbeddingScore", score);

		final int bin = FeatureUtils.getBin(bins, score);
		if (useBins) {
			for (int i = 0; i < bin; i++) {
				featureVector.set("WordEntityEmbeddingScore >= " + i, 1d);
			}

			featureVector.set("WordEntityEmbeddingScore" + bin, 1d);
			featureVector.set("WordEntityEmbeddingScore" + bin, score);
		}

	}

	public static double[] getVectorForURI(String uri) {
		return vectors.getWordVector(getURLForURI(uri.trim()));
	}

	public static double[] getVectorForSurfaceForm(String surfaceForm) {
		List<String> words = tokenize(surfaceForm.toLowerCase().trim());
		return getVectorForWords(words);
	}

	public static double[] getVectorForWords(List<String> words) {
		double[] vector = null;
		for (String w : words) {
			if (vectors.hasWord(w)) {
				if (vector == null) {
					vector = vectors.getWordVector(w);
				} else {
					vector = sum(vector, vectors.getWordVector(w));
				}
			}
		}
		return vector;
	}

	private static List<String> tokenize(String surfaceForm) {
		Matcher m = TOKENIZATION_PATTERN.matcher(surfaceForm);
		List<String> tokens = new ArrayList<>();
		while (m.find()) {
			String token = m.group();
			if (token.length() == 0)
				System.out.println("\n\n\n\n EMPTY TOKEN \n\n\n\n");
			tokens.add(token);
		}
		return tokens;
	}

	public static String getURLForURI(String uri) {
		return NAMESPACE + uri;
	}

	public static double cosineSimilarity(double[] v1, double[] v2) {
		double score = 0;
		double l1 = 0;
		double l2 = 0;
		for (int i = 0; i < v1.length; i++) {
			score += v1[i] * v2[i];
			l1 += Math.pow(v1[i], 2);
			l2 += Math.pow(v2[i], 2);
		}
		l1 = Math.sqrt(l1);
		l2 = Math.sqrt(l2);
		score /= l1 * l2;
		return score;
	}

	private static double[] sum(double[] v1, double[] v2) {
		double[] sum = new double[v1.length];
		for (int i = 0; i < v1.length; i++) {
			sum[i] = v1[i] + v2[i];
		}
		return sum;
	}

}
