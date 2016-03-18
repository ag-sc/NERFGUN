package de.citec.sc.learning;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import factors.Factor;
import learning.Vector;

public class FeatureUtils {

	public static String MIN_PREFIX = "MIN_";
	public static String MAX_PREFIX = "MAX_";
	public static String SUM_PREFIX = "SUM_";
	public static String AVRG_PREFIX = "AVRG_";
	//
	// public static void main(String[] args) {
	// Set<Factor<?>> factors = new HashSet<>();
	// Factor<?> f1 = new Factor<>(null);
	// f1.getFeatureVector().set("BANANA_RATE", 0.8);
	// f1.getFeatureVector().set("BANANA_WIDTH", 3.1);
	// f1.getFeatureVector().set("TopicSpecificBananaRank", 1.4);
	// factors.add(f1);
	//
	// Factor<?> f2 = new Factor<>(null);
	// f2.getFeatureVector().set("BANANA_RATE", 0.1);
	// f2.getFeatureVector().set("BANANA_WIDTH", -0.5);
	// f2.getFeatureVector().set("TopicSpecificBananaRank", 0.);
	// factors.add(f2);
	//
	// Factor<?> f3 = new Factor<>(null);
	// f3.getFeatureVector().set("BANANA_RATE", -3.);
	// f3.getFeatureVector().set("BANANA_WIDTH", 2.4);
	// f3.getFeatureVector().set("BANANA_BITS", 8.);
	// factors.add(f3);
	//
	// mergeFeatures(factors).getFeatures().entrySet().forEach(e ->
	// System.out.println(e));
	// }

	public static Vector mergeFeatures(Set<Factor<?>> factors) {
		Vector merged = new Vector();

		Vector min = new Vector();
		Vector max = new Vector();
		Vector sum = new Vector();
		Vector avrg = new Vector();

		for (Factor<?> factor : factors) {
			for (Entry<String, Double> feature : factor.getFeatureVector().getFeatures().entrySet()) {
				String featureName = feature.getKey();
				String minFeatureName = MIN_PREFIX + featureName;
				String maxFeatureName = MAX_PREFIX + featureName;
				String sumFeatureName = SUM_PREFIX + featureName;
				String avrgFeatureName = AVRG_PREFIX + featureName;

				if (min.getFeatureNames().contains(minFeatureName)) {
					min.set(minFeatureName, Math.min(min.getValueOfFeature(minFeatureName), feature.getValue()));
				} else {
					min.set(minFeatureName, feature.getValue());
				}
				if (max.getFeatureNames().contains(maxFeatureName)) {
					max.set(maxFeatureName, Math.max(max.getValueOfFeature(maxFeatureName), feature.getValue()));
				} else {
					max.set(maxFeatureName, feature.getValue());
				}
				sum.addToValue(sumFeatureName, feature.getValue());

				avrg.addToValue(avrgFeatureName, feature.getValue());

			}
		}
		for (Entry<String, Double> feature : avrg.getFeatures().entrySet()) {
			avrg.set(feature.getKey(), feature.getValue() / factors.size());
		}
		merged.add(min);
		merged.add(max);
		merged.add(sum);
		merged.add(avrg);
		return merged;
	}
}
