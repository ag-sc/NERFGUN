/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.variables.State;
import factors.AbstractFactor;
import factors.impl.SingleVariableFactor;
import learning.Vector;
import utility.VariableID;

/**
 *
 * @author sherzod
 */
public class IndexRankTemplate extends templates.AbstractTemplate<State> {

	private static Logger log = LogManager.getFormatterLogger();

	public IndexRankTemplate() {
	}

	@Override
	protected Collection<AbstractFactor> generateFactors(State state) {
		Set<AbstractFactor> factors = new HashSet<>();
		for (VariableID entityID : state.getEntityIDs()) {
			factors.add(new SingleVariableFactor(this, entityID));
		}
		return factors;
	}

	@Override
	protected void computeFactor(State state, AbstractFactor absFactor) {
		if (absFactor instanceof SingleVariableFactor) {
			SingleVariableFactor factor = (SingleVariableFactor) absFactor;
			Annotation entity = state.getEntity(factor.entityID);
			log.info("Compute IndexRank factor for state %s and variable %s", state.getID(), entity);

			// String uri =
			// entity.getLink().replace("http://dbpedia.org/resource/", "");
			Vector featureVector = new Vector();
			int rank = entity.getIndexRank();

			featureVector.set("IndexRank", (double) rank);
			int[] rankBins = { 1, 2, 3, 5, 10, 25, 50, 75, 90 };
			for (int rankBin : rankBins) {
				featureVector.set("IndexRank < " + rankBin, rank < rankBin);
			}
			int lastBin = rankBins[rankBins.length - 1];
			featureVector.set("IndexRank >= " + lastBin, rank >= lastBin);
			factor.setFeatures(featureVector);
		}
	}

}
