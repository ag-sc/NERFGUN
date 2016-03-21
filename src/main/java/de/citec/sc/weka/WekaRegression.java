package de.citec.sc.weka;

import de.citec.sc.formats.bire.BireDataLine;
import de.citec.sc.learning.FeatureUtils;
import factors.Factor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import learning.Vector;
import learning.scorer.Scorer;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.SparseInstance;

public class WekaRegression extends Scorer {

    Classifier model;
    
    public WekaRegression(Classifier model) {
        if (!(model instanceof LibSVM)) {
            throw new IllegalArgumentException("Wrong model used for regression.");
        }

        this.model = (LibSVM) model;
    }

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

    @Override
    public double score(Set<Factor<?>> factors) {
        Vector features = FeatureUtils.mergeFeatures(factors);

        BireDataLine b1 = new BireDataLine(0.0, features.getFeatures());
        
        List<BireDataLine> featureVectors = new ArrayList<>();
        featureVectors.add(b1);
        
        int vectorSize = BireDataLine.ALL_FEATURE_NAMES.size() + 1;
        Set<String> allFeatureNames = BireDataLine.ALL_FEATURE_NAMES;

        WekaInstanceBuilder wIB = new WekaInstanceBuilder(vectorSize, allFeatureNames, featureVectors);

        final Instances wrapper = wIB.createInstanceWrapper("BIRETestData");

        
            final SparseInstance dataPoint = wIB.buildSparseInstance(testData.getValue().featureVector);
            final double predictedScore;
            dataPoint.setDataset(wrapper);
            wrapper.add(dataPoint);
            predictedScore = model.classifyInstance(dataPoint);
        
    }

}
