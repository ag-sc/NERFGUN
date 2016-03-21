package de.citec.sc.formats.bire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BireDataLine {

	public final double classScore;

	public final static Set<String> ALL_FEATURE_NAMES = new HashSet<>();

	public final Map<String, Double> featureVector = new HashMap<String, Double>();

	public BireDataLine(double score, Map<String, Double> featureVector) {
		this.classScore = score;
		this.featureVector.putAll(featureVector);
		ALL_FEATURE_NAMES.addAll(featureVector.keySet());
	}

}
