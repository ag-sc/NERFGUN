/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;

/**
 *
 * @author sherzod
 */
public class LuceneScoreTemplate
		extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

	private static org.apache.logging.log4j.Logger log = LogManager.getFormatterLogger();

	CandidateRetriever indexSearch;

	public LuceneScoreTemplate(CandidateRetriever i) {
		indexSearch = i;
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
		log.debug("Compute %s factor for variable %s", LuceneScoreTemplate.class.getSimpleName(), entity);
		Vector featureVector = factor.getFeatureVector();

		// String luceneScorePrefix = "Relative TF (URI, label)";

		// featureVector.set(luceneScorePrefix, entity.getIndexScore());

		featureVector.set("Relative_TF", entity.getRelativeTermFrequencyScore());
		featureVector.set("Relative_PR", entity.getPageRankScore());
		featureVector.set("Relative String Similarity", entity.getStringSimilarity());

		// bins
		//// for(double i=0.01; i<1.0; i=i+0.01){
		//// featureVector.set("Relative_String_Similarity_HIGHER_THAN_"+i,
		//// entity.getStringSimilarity() > i ? 1.0 : 0);
		//// }

	}

}
