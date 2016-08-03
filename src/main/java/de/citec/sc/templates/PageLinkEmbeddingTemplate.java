/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.FeatureUtils;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.VariablePairPattern;
import learning.Vector;
import templates.AbstractTemplate;

/**
 *
 * Computes factors for pair-wise scores between entities based on similarities
 * of wikipedia pagelink embedding vectors.
 *
 * @author sjebbara
 *
 *         Jul 27, 2016
 */
public class PageLinkEmbeddingTemplate extends AbstractTemplate<Document, State, VariablePairPattern<Annotation>> {

	/*
	 * Handmade
	 */
	final private static int NUMBER_OF_BINS = 100;

	private static double[] bins = FeatureUtils.initializeBins(NUMBER_OF_BINS);

	private static Logger log = LogManager.getFormatterLogger();

	private static boolean isInitialized = false;

	private boolean useBins = false;
	private static WordVectors vectors = null;
	private static Map<String, Integer> keymap = null;

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static void init(final String keymapFilename, final String embeddingFilename) throws IOException {
		if (!isInitialized) {
			File gModel = new File(embeddingFilename);
			log.info("loading embeddings...");
			vectors = WordVectorSerializer.loadGoogleModel(gModel, true);
			log.info("Done, loading embeddings");
			log.info("loading keymap...");
			keymap = loadKeymap(keymapFilename);
			log.info("Done, loading keymap");
			isInitialized = true;
		}
	}

	private static Map<String, Integer> loadKeymap(String keymapFilename) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(keymapFilename));
		String line = null;
		int i = 0;
		Map<String, Integer> keymap = new HashMap<>();
		try {
			while ((line = r.readLine()) != null) {
				String[] parts = line.split(" ");
				if (parts.length != 2) {
					throw new IOException(String.format("Line %s contains more than 2 entries.", i));
				}
				int index = Integer.parseInt(parts[0]);
				String id = parts[1];
				keymap.put(id, index);
				i++;
			}
		} finally {
			r.close();
		}
		return keymap;
	}

	public PageLinkEmbeddingTemplate(boolean b) throws InitializationException {
		if (!isInitialized) {
			log.warn("PageLinkEmbeddingTemplate is NOT initialized correctly!");
			log.warn("Call PageLinkEmbeddingTemplate.init() for proper initlialization.");
			throw new InitializationException(
					"PageLinkEmbeddingTemplate is NOT initialized correctly! Call PageLinkEmbeddingTemplate.init() for proper initlialization.");
		}

		this.useBins = b;
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
		Annotation firstAnnotation = factor.getFactorPattern().getVariable1();
		Annotation secondAnnotation = factor.getFactorPattern().getVariable2();
		log.debug("Compute %s factor for variables %s and %s", PageLinkEmbeddingTemplate.class.getSimpleName(),
				firstAnnotation, secondAnnotation);

		Vector featureVector = factor.getFeatureVector();

		double score = 0;

		final String link1 = firstAnnotation.getLink();
		final String link2 = secondAnnotation.getLink();

		final String key1 = getKeyForURI(link1);
		final String key2 = getKeyForURI(link2);

		if (key1 != null && key2 != null && vectors.hasWord(key1) && vectors.hasWord(key2)) {
			score = score(link1, link2);
		} else {
			score = 0;
		}
		featureVector.set("PageLinkEmbeddingScore", score);

		final int bin = FeatureUtils.getBin(bins, score);
		if (useBins) {
			for (int i = 0; i < bin; i++) {
				featureVector.set("PageLinkEmbeddingScore >= " + i, 1d);
			}

			featureVector.set("PageLinkEmbeddingScore" + bin, 1d);
			featureVector.set("PageLinkEmbeddingScore" + bin, score);
		}

	}

	private static String getKeyForURI(String uri) {
		uri = uri.trim();
		if (keymap.containsKey(uri)) {
			return String.valueOf(keymap.get(uri));
		} else {
			return null;
		}
	}
	// private static double[] getVector(String uri) {
	// final int key = keymap.get(uri);
	// double[] v = vectors.getWordVector(String.valueOf(key));
	// return v;
	// }

	public static double score(String uri1, String uri2) {
		final String key1 = getKeyForURI(uri1);
		if (key1 == null)
			return 0.0;

		final String key2 = getKeyForURI(uri2);
		if (key2 == null)
			return 0.0;

		if (!vectors.hasWord(key1)) {
			return 0.0;
		}
		if (!vectors.hasWord(key2)) {
			return 0.0;
		} else {
			return vectors.similarity(key1, key2);
		}
	}

}
