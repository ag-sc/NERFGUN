package de.citec.sc.weka;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.citec.sc.helper.FeatureUtils;
import factors.Factor;
import learning.Vector;
import learning.scorer.AbstractSingleStateScorer;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffLoader.ArffReader;

public class WekaRegression extends AbstractSingleStateScorer {

    Classifier model;
    Set<String> featureNames;
    WekaInstanceBuilder wIB;

    public WekaRegression(String pathModel, String pathArffFile) {
        
        try {
            System.out.print("Loading libsvm model   ");
            this.model = WekaModelLoader.loadLibSVMModel(pathModel);
            
            
            System.out.println("DONE");
        } catch (Exception ex) {
            Logger.getLogger(WekaRegression.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        if (!(model instanceof LibSVM)) {
            throw new IllegalArgumentException("Wrong model used for regression.");
        }

        featureNames = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pathArffFile));
            ArffReader arff = new ArffReader(reader);

            for (int att = 0; att < arff.getStructure().numAttributes()-1; att++) {
                Attribute attribute = arff.getStructure().attribute(att);
                
                featureNames.add(attribute.name());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        wIB = new WekaInstanceBuilder(this.featureNames.size()+1, this.featureNames);

    }

  

    @Override
    public double score(Set<Factor<?>> factors) {
        Vector features = FeatureUtils.mergeFeatures(factors);


        //int vectorSize = BireDataLine.ALL_FEATURE_NAMES.size() + 1;
        //Set<String> allFeatureNames = BireDataLine.ALL_FEATURE_NAMES;

        //factors.stream().forEach(System.out::println);
        //System.out.println(factors);
        
        //System.out.println("Features : ");
        //features.getFeatures().entrySet().stream().forEach(System.out::println);

        Instances wrapper = wIB.createInstanceWrapper("TestOnModel");

        SparseInstance dataPoint = wIB.buildSparseInstance(features.getFeatures());
        
        dataPoint.setDataset(wrapper);
        wrapper.add(dataPoint);
        
        double predictedScore = 0.0;
        
        try {
            predictedScore = model.classifyInstance(dataPoint);
        } catch (Exception ex) {
            Logger.getLogger(WekaRegression.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return predictedScore;

    }


}
