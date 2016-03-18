package de.citec.sc.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.citec.sc.formats.bire.BireDataLine;
import weka.core.Instances;

/**
 * This class converts a given dataset in BIRE-Format to WEKA format.
 * 
 * @author hterhors
 *
 *         Mar 18, 2016
 */
public class BIRE2WEKADataConverter {

	private static final String BIREDataFileName = "BIRETrainingData";

	public static void main(String[] args) throws IOException {

		List<BireDataLine> d = readBIREDataFromFile(BIREDataFileName);

		convert2WEKA(d);

	}

	public static List<BireDataLine> readBIREDataFromFile(final String fileName) throws IOException {
		List<BireDataLine> data = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(new File(fileName)));

		String line;

		while ((line = br.readLine()) != null) {

			if (line.startsWith("#"))
				continue;

			data.add(convertStringToBireDataLine(line));

		}

		br.close();

		return data;
	}

	private static BireDataLine convertStringToBireDataLine(final String line) {
		String data[] = line.split("\t");

		final double score = Double.parseDouble(data[0]);
		Map<String, Double> featureVector = new HashMap<>();

		for (int i = 1; i < data.length;) {
			final String featureName = data[i++];
			final double featureValue = Double.parseDouble(data[i++]);
			featureVector.put(featureName, featureValue);
		}

		BireDataLine bireDataLine = new BireDataLine(score, featureVector);

		return bireDataLine;
	}

	public static void convert2WEKA(final List<BireDataLine> data) throws IOException {
		/*
		 * Add 1 for class label (score)
		 */
		final int featureVectorSize = 1 + BireDataLine.ALL_FEATURE_NAMES.size();

		final String instancesLabel = "BIRETrainingData";

		final Set<String> allFeatureNames = BireDataLine.ALL_FEATURE_NAMES;

		WekaInstanceBuilder wIB = new WekaInstanceBuilder(featureVectorSize, allFeatureNames, data);

		Instances BIRETrainingInstances = wIB.buildBatch(instancesLabel);

		WekaInstanceBuilder.writeBinaryInstancesToARFFFile(instancesLabel, BIRETrainingInstances);

	}

}
