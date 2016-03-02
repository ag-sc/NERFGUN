package de.citec.sc.learning;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.variables.State;
import learning.ObjectiveFunction;

/**
 *
 * @author sjebbara
 */
public class DisambiguationObjectiveFunction extends ObjectiveFunction<State, List<Annotation>> {

	private static Logger log = LogManager.getFormatterLogger();

	@Override
	protected double computeScore(State state, List<Annotation> goldResult) {
		long t = System.nanoTime();
		Set<Annotation> goldAnnotations = new HashSet<>(goldResult);
		Set<Annotation> predictedAnnotations = new HashSet<>(state.getEntities());

		Set<Annotation> overlap = Sets.intersection(goldAnnotations, predictedAnnotations);
		double correct = overlap.size();
		double total = goldAnnotations.size();
		double score = correct / total;
		return score;
	}

}
