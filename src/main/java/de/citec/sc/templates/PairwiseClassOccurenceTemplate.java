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
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.VariablePairPattern;
import java.util.List;
import learning.Vector;

/**
 *
 * Computes a score given all entities and their topic specific pageranks of
 * each other.
 *
 * @author hterhors
 *
 * Feb 18, 2016
 */
public class PairwiseClassOccurenceTemplate  extends templates.AbstractTemplate<Document, State, VariablePairPattern<Annotation>> {

private static Logger log = LogManager.getFormatterLogger();


    public PairwiseClassOccurenceTemplate() {
        
    }

    
    @Override
    public Set<VariablePairPattern<Annotation>> generateFactorPatterns(State state) {
        Set<VariablePairPattern<Annotation>> factors = new HashSet<>();
        for (Annotation firstAnnotation : state.getEntities()) {
            for (Annotation secondAnnotation : state.getEntities()) {
                if (!firstAnnotation.equals(secondAnnotation)) {
                    factors.add(new VariablePairPattern<>(this, firstAnnotation, secondAnnotation));
                }
            }
        }

        log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
        return factors;
    }

    @Override
    public void computeFactor(Document instance, Factor<VariablePairPattern<Annotation>> factor) {
        Annotation firstAnnotation = factor.getFactorPattern().getVariable1();
        Annotation secondAnnotation = factor.getFactorPattern().getVariable2();
        log.debug("Compute %s factor for variables %s and %s", PairwiseClassOccurenceTemplate.class.getSimpleName(),
                firstAnnotation, secondAnnotation);

        Vector featureVector = factor.getFeatureVector();

        final String link1 = firstAnnotation.getLink().trim();
        final String link2 = secondAnnotation.getLink().trim();
        
        Set<String> setOfClasses1 = DBpediaEndpoint.getClasses(link1);
        Set<String> setOfClasses2 = DBpediaEndpoint.getClasses(link2);
        
        for(String c1 : setOfClasses1){
            for(String c2 : setOfClasses2){
                
                featureVector.set("Class_"+c1.replace("http://dbpedia.org/ontology/", "")+" co-occurs with Class_" + c2.replace("http://dbpedia.org/ontology/", ""), 1d);
                if(c1.equals(c2)){
                    featureVector.set("Class_"+c1.replace("http://dbpedia.org/ontology/", "")+" also another entiy belongs to Class_" + c2.replace("http://dbpedia.org/ontology/", ""), 1d);
                }
            }
        }
    }
}
