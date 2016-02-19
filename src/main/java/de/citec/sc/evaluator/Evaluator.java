/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.evaluator;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class Evaluator {

    private Map<String, Integer> calculate(Document d) {
        Map<String, Integer> result = new LinkedHashMap<>();

        List<Annotation> annotations = d.getAnnotations();
        List<Annotation> goldStandard = d.getGoldStandard();

        int TP = 0;
        int FN = 0;
        for (Annotation g : goldStandard) {
            if (annotations.contains(g)) {
                TP++;
            } else {
                FN++;
            }
        }

        int FP = annotations.size() - TP;

        result.put("FP", FP);
        result.put("TP", TP);
        result.put("FN", FN);
        result.put("TN", 0);

        return result;
    }

    public Map<String, Double> evaluate(Document document) {
        Map<String, Double> result = new HashMap<>();

        Map<String, Integer> numbers = calculate(document);
        int TP = 0, FP = 0, FN = 0, TN = 0;

        for (String n : numbers.keySet()) {

            if (n.equals("TP")) {
                TP = numbers.get(n);
            }
            if (n.equals("FP")) {
                FP = numbers.get(n);
            }
            if (n.equals("FN")) {
                FN = numbers.get(n);
            }
            if (n.equals("TN")) {
                TN = numbers.get(n);
            }

        }
        //calculate precision and recall for each document
        double r = getRecall(TP, FN);
        double p = getPrecision(TP, FP);
        double F1 = getF1(p, r);

        result.put("Precision", round(p, 3));
        result.put("Recall", round(r, 3));
        result.put("F1", round(F1, 3));

        return result;
    }

    public Map<String, Double> evaluateAll(List<Document> documents) {
        Map<String, Double> result = new LinkedHashMap<>();

        int sumOfTP = 0, sumOfFP = 0, sumOfFN = 0, sumOfTN = 0;

        double macroAvgPrecision = 0, macroAvgRecall = 0;
        double microAvgPrecision = 0, microAvgRecall = 0;

        for (Document d : documents) {

            Map<String, Integer> numbers = calculate(d);
            int TP = 0, FP = 0, FN = 0, TN = 0;

            for (String n : numbers.keySet()) {

                if (n.equals("TP")) {
                    sumOfTP += numbers.get(n);
                    TP = numbers.get(n);
                }
                if (n.equals("FP")) {
                    sumOfFP += numbers.get(n);
                    FP = numbers.get(n);
                }
                if (n.equals("FN")) {
                    sumOfFN += numbers.get(n);
                    FN = numbers.get(n);
                }
                if (n.equals("TN")) {
                    sumOfTN += numbers.get(n);
                    TN = numbers.get(n);
                }

            }
            //calculate precision and recall for each document
            double r = getRecall(TP, FN);
            double p = getPrecision(TP, FP);

            //sum of precision and recall for each document
            macroAvgPrecision += p;
            macroAvgRecall += r;
        }

        //calculate average of precision and recall for Macro Average
        macroAvgPrecision = macroAvgPrecision / documents.size();
        macroAvgRecall = macroAvgRecall / documents.size();

        //calculate Micro Average Precision and recall
        microAvgPrecision = getPrecision(sumOfTP, sumOfFP);
        microAvgRecall = getRecall(sumOfTP, sumOfFN);

        double F1_macro = getF1(macroAvgPrecision, macroAvgRecall);
        double F1_micro = getF1(microAvgPrecision, microAvgRecall);

        result.put("Micro-average Precision", round(microAvgPrecision, 3));
        result.put("Micro-average Recall", round(microAvgRecall, 3));
        result.put("F1 Micro-average", round(F1_micro, 3));

        result.put("Macro-average Precision", round(macroAvgPrecision, 3));
        result.put("Macro-average Recall", round(macroAvgRecall, 3));
        result.put("F1 Macro-average", round(F1_macro, 3));

        return result;
    }

    private double getRecall(int TP, int FN) {
        double r = TP / (double) (FN + TP);
        if (TP == 0 && FN == 0) {
            r = 0;
        }
        return r;
    }

    private double getPrecision(int TP, int FP) {
        double p = TP / (double) (TP + FP);
        if (TP == 0 && FP == 0) {
            p = 0;
        }
        return p;
    }

    private double getF1(double precision, double recall) {
        return (2 * precision * recall) / (precision + recall);
    }

    private double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
