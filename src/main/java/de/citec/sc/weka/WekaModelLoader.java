package de.citec.sc.weka;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;

public class WekaModelLoader {

	public static Classifier loadLibSVMModel(final String filenName) throws Exception {

		final LibSVM svm = (LibSVM) weka.core.SerializationHelper.read(filenName);
                
		return svm;

	}

}
