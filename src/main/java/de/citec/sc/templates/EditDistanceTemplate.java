/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;

/**
 * 
 * Computes a similarity score given the distance of two strings weighted by
 * their length. The longer the strings and the lower the edit distance the
 * higher the similarity.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class EditDistanceTemplate
		extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {
	private static Logger log = LogManager.getFormatterLogger();

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
		log.debug("Compute %s factor for variable %s", EditDistanceTemplate.class.getSimpleName(), entity);
		Vector featureVector = factor.getFeatureVector();

		log.debug("Retrieve text for query link %s...", entity.getLink());

		final String link = entity.getLink().replaceAll("_", " ").toLowerCase();
		final String word = entity.getWord().toLowerCase();

		final int levenDist = SimilarityMeasures.levenshteinDistance(link, word);

		final int max = Math.max(link.length(), word.length());

		final double weightedEditSimilarity = ((double) (max - levenDist) / (double) max);

		featureVector.set("-0.5_LevenshteinEditSimilarity", weightedEditSimilarity - 0.5);
		featureVector.set("Positive_LevenshteinEditSimilarity", weightedEditSimilarity);

		for (double i = 0.01; i < 1.0; i = i + 0.01) {
			featureVector.set("LevenshteinEditSimilarity_bin_" + i, weightedEditSimilarity > i ? 1.0 : 0);
		}

	}

}
