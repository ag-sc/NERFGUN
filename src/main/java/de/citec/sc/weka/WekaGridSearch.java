/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.weka;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.meta.GridSearch;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SelectedTag;

/**
 *
 * @author sherzod
 */
public class WekaGridSearch {

    public static void main(String[] args) {

        try {
            // TODO code application logic here
            BufferedReader trainreader = new BufferedReader(new FileReader("features_PR-TF-ED-TSPR-DS.arff"));
            Instances traindata = new Instances(trainreader);
            trainreader.close();
            System.out.println("Read train data");

            traindata.setClassIndex(traindata.numAttributes() - 1);
            //Classifier classifer = new NaiveBayes();
            //SMOreg reg  = new SMOreg();

            LibSVM reg = new LibSVM();
            reg.setSVMType(new SelectedTag(LibSVM.SVMTYPE_EPSILON_SVR, LibSVM.TAGS_SVMTYPE));
            reg.setCost(1.0);
            reg.setGamma(0.01);

            GridSearch search = new GridSearch();
            search.setClassifier(new LinearRegression());
            int requiredIndex = 6; // for accuracy
            SelectedTag st = new SelectedTag(requiredIndex, weka.classifiers.meta.GridSearch.TAGS_EVALUATION);
            search.setEvaluation(st);

            //RBFKernel kernel = new RBFKernel();
            //kernel.setGamma(0.01);
            //reg.setC(1.0);
            //reg.setKernel(kernel);
            System.out.println("Starting to train");
            search.buildClassifier(traindata);

            search.toString();

        } catch (IOException ex) {
        } catch (Exception ex) {
            Logger.getLogger(WekaGridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
