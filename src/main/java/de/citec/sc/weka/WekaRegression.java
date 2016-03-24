package de.citec.sc.weka;

import de.citec.sc.formats.bire.BireDataLine;
import de.citec.sc.learning.FeatureUtils;
import factors.Factor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import learning.Vector;
import learning.scorer.Scorer;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffLoader.ArffReader;

public class WekaRegression extends Scorer {

    Classifier model;
    Set<String> featureNames;

    public WekaRegression(String pathModel, String path) {
        
        try {
            this.model = WekaModelLoader.loadLibSVMModel(pathModel);
        } catch (Exception ex) {
            Logger.getLogger(WekaRegression.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (!(model instanceof LibSVM)) {
            throw new IllegalArgumentException("Wrong model used for regression.");
        }

        featureNames = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            ArffReader arff = new ArffReader(reader);

            for (int att = 0; att < arff.getStructure().numAttributes(); att++) {
                Attribute attribute = arff.getStructure().attribute(att);
                
                featureNames.add(attribute.name());
            }
        } catch (IOException e) {

        }

    }

  

    @Override
    public double score(Set<Factor<?>> factors) {
        Vector features = FeatureUtils.mergeFeatures(factors);


        //int vectorSize = BireDataLine.ALL_FEATURE_NAMES.size() + 1;
        //Set<String> allFeatureNames = BireDataLine.ALL_FEATURE_NAMES;

        WekaInstanceBuilder wIB = new WekaInstanceBuilder(this.featureNames.size()+1, this.featureNames);

        Instances wrapper = wIB.createInstanceWrapper("TestOnModel");

        SparseInstance dataPoint = wIB.buildSparseInstance(features.getFeatures());
        double predictedScore = 0.0;
        dataPoint.setDataset(wrapper);
        wrapper.add(dataPoint);
        try {
            predictedScore = model.classifyInstance(dataPoint);
        } catch (Exception ex) {
            Logger.getLogger(WekaRegression.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return predictedScore;

    }

}
