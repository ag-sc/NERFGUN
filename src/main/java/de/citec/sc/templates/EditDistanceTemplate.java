/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import java.util.ArrayList;
import java.util.List;
import learning.Vector;

/**
 *
 * Computes a similarity score given the distance of two strings weighted by
 * their length. The longer the strings and the lower the edit distance the
 * higher the similarity.
 *
 * @author hterhors
 *
 * Feb 18, 2016
 */
public class EditDistanceTemplate
        extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

    private static Logger log = LogManager.getFormatterLogger();

    private boolean useBins = false;

    public EditDistanceTemplate(boolean b) {
        this.useBins = b;
    }

//    public EditDistanceTemplate() {
//    }
    @Override
    public Set<SingleVariablePattern<Annotation>> generateFactorPatterns(State state) {
        Set<SingleVariablePattern<Annotation>> factors = new HashSet<>();
        for (Annotation a : state.getEntities()) {
            factors.add(new SingleVariablePattern<>(this, a));
        }
        log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
        return factors;
    }

    @Override
    public void computeFactor(Document instance, Factor<SingleVariablePattern<Annotation>> factor) {
        Annotation entity = factor.getFactorPattern().getVariable();
        log.debug("Compute %s factor for variable %s", EditDistanceTemplate.class.getSimpleName(), entity);
        Vector featureVector = factor.getFeatureVector();

        log.debug("Retrieve text for query link %s...", entity.getLink());

        double weightedEditSimilarity = 0, value = 0;

        try {
            
            String link = entity.getLink();
            
            if(link.contains("_(") && link.endsWith(")")){
                link = link.substring(0, link.indexOf("_("));
            }
            
            link = link.replaceAll("_", " ").toLowerCase();
            
            
            final String word = entity.getWord().toLowerCase();

            final int levenDist = SimilarityMeasures.levenshteinDistance(link, word);

            final int max = Math.max(link.length(), word.length());

            weightedEditSimilarity = ((double) (max - levenDist) / (double) max);

//            value = Math.pow(weightedEditSimilarity + 1, 2);
//
//            //hacking features
//            if (entity.getLink().contains("_(number)")) {
//                if (isNumber(word)) {
////                    featureVector.set("NumberFeature", 2d);
//                }
//            }
            
            if(isAbbreviation(word, link)){
                featureVector.set("Abbreviation Feature", 1.0);
            }

        } catch (Exception e) {
            log.info("Link " + entity.getLink() + "\n");
            log.info("Word " + entity.getWord() + "\n");
            log.info(e.getMessage() + "\n");
        }

        featureVector.set("-0.5_LevenshteinEditSimilarity", weightedEditSimilarity - 0.5);
        featureVector.set("Positive_LevenshteinEditSimilarity", weightedEditSimilarity);
        
        
//        featureVector.set("Positive_LevenshteinEditSimilarity_Pow", value);

        if (useBins) {
            for (double i = 0.01; i < 1.0; i = i + 0.01) {
                featureVector.set("LevenshteinEditSimilarity_bin_" + i, weightedEditSimilarity > i ? 1.0 : 0);
            }
        }

    }
    
    private boolean isAbbreviation(String node, String uri) {
        String abbr = node.length() > uri.length() ? uri : node;
        String word = node.length() > uri.length() ? node : uri;
        
        abbr = abbr.replace(".", "");

        String[] tokens = word.split(" ");
        
        if(tokens.length != abbr.length()){
            return false;
        }
        
        int count = 0;
        for (int i = 0; i < abbr.length(); i++) {
            String c = abbr.charAt(i) + "";
            if (tokens[i].startsWith(c)) {
                count++;
            }
        }

        if (count == abbr.length()) {
            return true;
        }

        return false;
    }

    private boolean isNumber(String s) {

        String regex = "\\d+";

        boolean b = s.matches(regex);

        List<String> numbers = new ArrayList<>();
        numbers.add("one");
        numbers.add("two");
        numbers.add("three");
        numbers.add("four");
        numbers.add("five");
        numbers.add("six");
        numbers.add("seven");
        numbers.add("eight");
        numbers.add("nine");
        numbers.add("ten");
        numbers.add("eleven");
        numbers.add("twelve");
        numbers.add("thirteen");

        if (b == false) {
            if (numbers.contains(s)) {
                return true;
            }
        }

        return false;
    }

}
