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
import de.citec.sc.helper.DBpediaEndpoint;
import de.citec.sc.helper.DocumentUtils;
import de.citec.sc.helper.NGramExtractor;
import de.citec.sc.helper.Stopwords;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import learning.Vector;

/**
 *
 * Adds properties and classes of the entity querying DBpedia
 *
 * @author sherzod
 *
 * Jul 5, 2016
 */
public class ClassContextTemplate
        extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

    private static Logger log = LogManager.getFormatterLogger();
    private static NGramExtractor ngramExtractor = new NGramExtractor();

    public ClassContextTemplate() {

    }

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
        log.debug("Compute %s factor for variable %s", ClassContextTemplate.class.getSimpleName(), entity);
        Vector featureVector = factor.getFeatureVector();

        log.debug("Retrieve classes and properties for query link %s...", entity.getLink());

        try {

            String link = entity.getLink();

            Set<String> classes = DBpediaEndpoint.getClasses(link);
            
            //reduce classes = > remove superclasses
            Set<String> reducedClasses = reduceClasses(classes, true);

            //take 5 words on left and 5 on the right
            String contextWords = getContextWords(instance.getDocumentContent(), entity.getStartIndex(), entity.getEndIndex(), 5);
            
            //extract ngrams: (uni, bi , tri), remove stopwords : true
            Set<String> ngrams = extractNgrams(contextWords, 3, true);

            for (String c : reducedClasses) {
                for(String n : ngrams){
                    featureVector.set("CLASSES Feature: " + c+" NGram: "+n, 1.0);
                }
            }

        } catch (Exception e) {
            log.info("Link " + entity.getLink() + "\n");
            log.info("Word " + entity.getWord() + "\n");
            log.info(e.getMessage() + "\n");
        }

    }

    private static Set<String> reduceClasses(Set<String> classes, boolean superClass) {
        Set<String> reducedClasses = new HashSet<>();

        for (String c : classes) {
            //if old class c is superClass of newClass then add the new one

            if (reducedClasses.isEmpty()) {
                reducedClasses.add(c);
            } else {
                Set<String> temp = new HashSet<>();

                for (String added : reducedClasses) {
                    if(DBpediaEndpoint.isSubClass(added, c)){
                        
                        //if superclass are preferred
                        if(superClass){
                            temp.add(added);
                        }
                        else{
                            temp.add(c);
                        }
                        
                    }
                    else if(DBpediaEndpoint.isSubClass(c, added)){
                        
                        //if superclass are preferred
                        if(superClass){
                            temp.add(c);
                        }
                        else{
                            temp.add(added);
                        }
                        
                    }
                    //not related , add both
                    else{
                        temp.add(c);
                        temp.add(added);
                    }
                }
                
                reducedClasses.clear();
                reducedClasses.addAll(temp);
            }
        }

        return reducedClasses;
    }

    private String getContextWords(String docText, int startIndex, int endIndex, int numberOfTokens) {
        String leftContextWords = "";
        String rightContextWords = "";

        String left = "", right = "";
        if (startIndex < docText.length()) {
            left = docText.substring(0, startIndex);
        }
        right = docText.substring(endIndex);

        String[] tokens = left.split(" ");

        int c = 0;
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (tokens[i].trim().length() > 2) {
                leftContextWords = tokens[i] + " " + leftContextWords;
                c++;
                if (c == numberOfTokens) {
                    break;
                }
            }

        }

        tokens = right.split(" ");

        c = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].trim().length() > 2) {
                rightContextWords += tokens[i] + " ";
                c++;
                if (c == numberOfTokens) {
                    break;
                }
            }
        }

        return leftContextWords.trim().toLowerCase() + " " + rightContextWords.trim().toLowerCase();
    }

    private static Set<String> extractNgrams(String text, int ngram_size, boolean removeStopWords) {
        Set<String> setOfNgrams = new HashSet<>();

        try {
            //extracts ngrams also removes stopwords if set to true
            //doesn't extract the unigrams
            ngramExtractor.extract(text.trim(), 2, ngram_size, removeStopWords);
            LinkedList<String> ngrams = ngramExtractor.getNGrams();

            //add ngrams
            for (String n : ngrams) {
                setOfNgrams.add(n.trim());
            }

            //add unigrams
            for (String n : text.split(" ")) {
                setOfNgrams.add(n.trim());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return setOfNgrams;
    }

}
