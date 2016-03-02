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
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;

/**
 *
 * @author sherzod
 */
public class IndexRankTemplate extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

	private static Logger log = LogManager.getFormatterLogger();

	private static int[] rankBins = { 1, 2, 3, 5, 10, 25, 50, 75, 90 };

	public IndexRankTemplate() {
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
		log.debug("Compute %s factor for variable %s", IndexRankTemplate.class.getSimpleName(), entity);
		Vector featureVector = factor.getFeatureVector();
		int rank = entity.getIndexRank();

		if (rank != -1) {
			featureVector.set("IndexRank", (double) rank);
			for (int rankBin : rankBins) {
				featureVector.set("IndexRank < " + rankBin, rank < rankBin);
			}
			int lastBin = rankBins[rankBins.length - 1];
			featureVector.set("IndexRank >= " + lastBin, rank >= lastBin);
		}
	}

}
