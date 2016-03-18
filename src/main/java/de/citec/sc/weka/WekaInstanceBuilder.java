package de.citec.sc.weka;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.citec.sc.formats.bire.BireDataLine;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

class WekaInstanceBuilder {

	private final int vectorSize;
	private final Set<String> allFeatureNames;
	private final List<BireDataLine> featureVectors;

	/**
	 * 
	 * @param vectorSize
	 *            total number of features +1 for class
	 */
	public WekaInstanceBuilder(final int vectorSize, final Set<String> allFeatureNames,
			final List<BireDataLine> featureVectors) {
		this.vectorSize = vectorSize;
		this.allFeatureNames = allFeatureNames;
		this.featureVectors = featureVectors;
	}

	public SparseInstance buildSparseInstance(final BireDataLine dataPoint) {
		return buildSparseInstance(dataPoint.featureVector);
	}

	public SparseInstance buildSparseInstance(final Map<String, Double> sortedFeatureVector) {

		final List<Double> valueList = new LinkedList<Double>();
		final List<Integer> indexList = new LinkedList<Integer>();
		int attributeIndex = 0;
		for (final String featureValue : allFeatureNames) {
			if (sortedFeatureVector.containsKey(featureValue)) {
				valueList.add(sortedFeatureVector.get(featureValue));
				indexList.add(attributeIndex);
			}
			attributeIndex++;
		}

		final double[] values = new double[valueList.size() + 1];
		final int[] indices = new int[valueList.size()];

		for (int i = 0; i < valueList.size(); i++) {
			values[i] = valueList.get(i);
			indices[i] = indexList.get(i);
		}
		return new SparseInstance(1.0d, values, indices, vectorSize);
	}

	public Instances createInstanceWrapper(final String wrapperName) {
		final FastVector v = new FastVector();

		for (String featureName : allFeatureNames) {
			v.addElement(new Attribute(featureName));
		}
		v.addElement(new Attribute("Score"));

		final Instances instances = new Instances(wrapperName, v, v.size());
		instances.setClassIndex(instances.numAttributes() - 1);
		return instances;
	}

	public Instances buildBatch(final String instancesLabel) {

		final Instances instances = createInstanceWrapper(instancesLabel);

		for (final BireDataLine bireDataLine : featureVectors) {

			final double score = bireDataLine.classScore;
			final Map<String, Double> featureVector = bireDataLine.featureVector;

			final Instance s = buildSparseInstance(featureVector);

			s.setDataset(instances);
			s.setClassValue(score);
			instances.add(s);
		}
		return instances;
	}

	public static void writeBinaryInstancesToARFFFile(String fileName, final Instances instances) throws IOException {
		fileName = fileName.substring(0, fileName.lastIndexOf(".")).concat(".arff");

		final ArffSaver saver = new ArffSaver();
		saver.setInstances(instances);
		saver.setFile(new File(fileName));
		saver.setDestination(new File(fileName));
		saver.writeBatch();
	}

}
