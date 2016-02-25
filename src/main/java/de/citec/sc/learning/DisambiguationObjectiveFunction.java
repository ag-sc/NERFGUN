package de.citec.sc.learning;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.variables.State;
import learning.ObjectiveFunction;

/**
 *
 * @author sjebbara
 */
public class DisambiguationObjectiveFunction extends ObjectiveFunction<State, List<Annotation>> {

	@Override
	protected double computeScore(State state, List<Annotation> goldResult) {
		Set<Annotation> goldAnnotations = new HashSet<>(goldResult);
		Set<Annotation> predictedAnnotations = new HashSet<>(state.getEntities());

		if (goldAnnotations.size() == 0 && predictedAnnotations.size() == 0) {
			return 1;
		} else if (goldAnnotations.size() == 0 && predictedAnnotations.size() != 0) {
			return 0;
		} else if (goldAnnotations.size() != 0 && predictedAnnotations.size() == 0) {
			return 0;
		}
		Set<Annotation> overlap = Sets.intersection(goldAnnotations, predictedAnnotations);

		double tp = overlap.size();
		double fp = predictedAnnotations.size() - overlap.size();
		double fn = goldAnnotations.size() - overlap.size();

		double precision = tp / (tp + fp);
		double recall = tp / (tp + fn);
		double f1 = 2 * precision * recall / (precision + recall);

		if (Double.isNaN(f1)) {
			return 0;
		}
		return f1;
	}

}
