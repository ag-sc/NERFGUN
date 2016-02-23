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
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
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
public class EditDistanceTemplate extends templates.AbstractTemplate<State> {

	private static Logger log = LogManager.getFormatterLogger();

	public EditDistanceTemplate() {

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
			log.debug("Compute DocumentSimilarity factor for state %s and variable %s", state.getID(), entity);
			Vector featureVector = new Vector();

			log.debug("Retrieve text for query link %s...", entity.getLink());

			double levenDist = SimilarityMeasures.levenshteinDistance(
					entity.getLink().replaceAll("_", " ").toLowerCase(), entity.getWord().toLowerCase());

			featureVector.set("EditDistance", levenDist);

			factor.setFeatures(featureVector);
		}
	}

}
