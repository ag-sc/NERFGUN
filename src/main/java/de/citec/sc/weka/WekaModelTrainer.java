/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.weka;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import test.TestWeka;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.meta.GridSearch;
import weka.core.Instances;
import weka.core.SelectedTag;

/**
 *
 * @author sherzod
 */
public class WekaModelTrainer {

    public static void train(String path) {
        LibSVM reg = new LibSVM();
        try {
            // TODO code application logic here
            
            System.out.println("Read train data");
            Instances traindata = BIRE2WEKADataConverter.getTrainingData(path);
            
            

            traindata.setClassIndex(traindata.numAttributes() - 1);
            //Classifier classifer = new NaiveBayes();
            //SMOreg reg  = new SMOreg();
            
            reg.setSVMType(new SelectedTag(LibSVM.SVMTYPE_EPSILON_SVR, LibSVM.TAGS_SVMTYPE));
            reg.setCost(1.0);
            reg.setGamma(0.01);

            //RBFKernel kernel = new RBFKernel();
            //kernel.setGamma(0.01);
            //reg.setC(1.0);
            //reg.setKernel(kernel);
            System.out.print("Starting to train      ");
            reg.buildClassifier(traindata);
            System.out.println("DONE");

            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(path.replace(".csv", "") + ".model"));
            oos.writeObject(reg);
            oos.flush();
            oos.close();
            
            System.out.println("Saved model for "+ path);

        } catch (IOException ex) {
            Logger.getLogger(TestWeka.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(WekaModelTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     
    }
}
