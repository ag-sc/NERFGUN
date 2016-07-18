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
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import java.util.ArrayList;
import java.util.List;
import learning.Vector;

/**
 *
 * Adds properties and classes of the entity querying DBpedia
 *
 * @author sherzod
 *
 * Jul 5, 2016
 */
public class ClassPropertyTemplate
        extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

    private static Logger log = LogManager.getFormatterLogger();

    

    public ClassPropertyTemplate() {
        
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
        log.debug("Compute %s factor for variable %s", ClassPropertyTemplate.class.getSimpleName(), entity);
        Vector featureVector = factor.getFeatureVector();

        log.debug("Retrieve classes and properties for query link %s...", entity.getLink());

        try {

            String link = entity.getLink();

            Set<String> properties = DBpediaEndpoint.getProperties(link);
            Set<String> classes = DBpediaEndpoint.getClasses(link);

            for (String p : properties) {
                featureVector.set("PROPERTIES Feature: " + p, 1.0);
            }
            for (String p : classes) {
                featureVector.set("CLASSES Feature: " + p, 1.0);
            }

        } catch (Exception e) {
            log.info("Link " + entity.getLink() + "\n");
            log.info("Word " + entity.getWord() + "\n");
            log.info(e.getMessage() + "\n");
        }

    }

}
