package de.citec.sc.evaluator;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PRF1 {

	public static <K, V> double objectiveFunction(Map<K, Set<V>> goldData, Map<K, Set<V>> resultData) {

		for (K abstarctID : goldData.keySet()) {
			resultData.putIfAbsent(abstarctID, new HashSet<V>());
		}

		double macroF1 = 0;
		for (K pubmedID : goldData.keySet()) {
			macroF1 += PRF1.f1(goldData.get(pubmedID), resultData.get(pubmedID)) / goldData.size();
			System.out.println("DocumentID		= " + pubmedID);
			System.out.println("Findings		= " + resultData.get(pubmedID));
			System.out.println("Gold			= " + goldData.get(pubmedID));
			System.out.println("Precision: " + PRF1.precision(goldData.get(pubmedID), resultData.get(pubmedID)));
			System.out.println("Recall: " + PRF1.recall(goldData.get(pubmedID), resultData.get(pubmedID)));
			System.out.println("F1: " + PRF1.f1(goldData.get(pubmedID), resultData.get(pubmedID)));
			System.out.println();
		}

		return macroF1;
	}

	public static <K, V> double calculate(Map<K, Set<V>> goldData, Map<K, Set<V>> baselineData) {
		return calculate(goldData, baselineData, System.out);
	}

	public static <K, V> double calculate(Map<K, Set<V>> goldData, Map<K, Set<V>> baselineData, PrintStream logging) {

		for (K abstarctID : goldData.keySet()) {
			baselineData.putIfAbsent(abstarctID, new HashSet<V>());
		}

		int tp = 0;
		int fp = 0;
		int fn = 0;
		double macroPrecision = 0;
		double macroRecall = 0;
		double macroF1 = 0;
		for (K pubmedID : goldData.keySet()) {

			tp += PRF1.getTruePositives(goldData.get(pubmedID), baselineData.get(pubmedID));

			fp += PRF1.getFalsePositives(goldData.get(pubmedID), baselineData.get(pubmedID));

			fn += PRF1.getFalseNegatives(goldData.get(pubmedID), baselineData.get(pubmedID));

			macroPrecision += PRF1.precision(goldData.get(pubmedID), baselineData.get(pubmedID)) / goldData.size();
			macroRecall += PRF1.recall(goldData.get(pubmedID), baselineData.get(pubmedID)) / goldData.size();
			macroF1 += PRF1.f1(goldData.get(pubmedID), baselineData.get(pubmedID)) / goldData.size();
		}

		logging.println("tp = " + round(tp));
		logging.println("fp = " + round(fp));
		logging.println("fn = " + round(fn));
		logging.println("Micro precision = " + round(PRF1.microPrecision(tp, fp, fn)));
		logging.println("Micro recall = " + round(PRF1.microRecall(tp, fp, fn)));
		logging.println("Micro F1 = " + (round(PRF1.microF1(tp, fp, fn))));
		logging.println();
		logging.println("Macro precision = " + round(macroPrecision));
		logging.println("Macro recall = " + round(macroRecall));
		logging.println("Macro F1 = " + round(macroF1));
		logging.println();
		logging.println();

		return macroF1;
	}

	private static <E> double round(double d) {
		try {

			return Double.valueOf(new DecimalFormat("#.###").format(d));
		} catch (Exception e) {
			System.out.println("Format " + d + " caused errors...");
			e.printStackTrace();
			throw e;
		}
	}

	public static <E> double microPrecision(double tp, double fp, double fn) {
		return tp / (tp + fp);
	}

	public static <E> double microRecall(double tp, double fp, double fn) {
		return tp / (tp + fn);
	}

	public static <E> double microF1(double tp, double fp, double fn) {
		double p = microPrecision(tp, fp, fn);
		double r = microRecall(tp, fp, fn);
		double f1 = 2 * (p * r) / (p + r);
		return f1;
	}

	public static <E> int getTruePositives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return intersection.size();
	}

	public static <E> int getFalsePositives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);

		return result.size() - intersection.size();

	}

	public static <E> int getFalseNegatives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);

		return gold.size() - intersection.size();

	}

	public static <E> double precision(Set<E> gold, Set<E> result) {

		if (result.size() == 0) {
			return 0;
		}

		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return (double) intersection.size() / result.size();

	}

	public static <E> double recall(Set<E> gold, Set<E> result) {

		if (gold.size() == 0) {
			return 0;
		}

		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return (double) intersection.size() / gold.size();

	}

	public static <E> double f1(Set<E> gold, Set<E> result) {
		double p = precision(gold, result);
		double r = recall(gold, result);
		if (p == 0 && r == 0) {
			return 0;
		}
		double f1 = 2 * ((p * r) / (p + r));
		return f1;

	}
}
