package de.citec.sc.weka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.citec.sc.formats.bire.BireDataLine;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.SparseInstance;

public class WekaRegression {

	public static void batchRegressionLibSVM(Classifier model, Map<String, BireDataLine> data)
			throws IllegalArgumentException, Exception {

		if (!(model instanceof LibSVM)) {
			throw new IllegalArgumentException("Wrong model used for regression.");
		}

		model = (LibSVM) model;

		int vectorSize = BireDataLine.ALL_FEATURE_NAMES.size() + 1;
		Set<String> allFeatureNames = BireDataLine.ALL_FEATURE_NAMES;
		List<BireDataLine> featureVectors = new ArrayList<>(data.values());
		WekaInstanceBuilder wIB = new WekaInstanceBuilder(vectorSize, allFeatureNames, featureVectors);

		final Instances wrapper = wIB.createInstanceWrapper("BIRETestData");

		for (Entry<String, BireDataLine> testData : data.entrySet()) {
			final SparseInstance dataPoint = wIB.buildSparseInstance(testData.getValue().featureVector);
			final double predictedScore;
			dataPoint.setDataset(wrapper);
			wrapper.add(dataPoint);
			predictedScore = model.classifyInstance(dataPoint);
		}

	}

}
