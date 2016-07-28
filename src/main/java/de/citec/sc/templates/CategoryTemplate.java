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
import static de.citec.sc.helper.DBpediaEndpoint.getCategories;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import factors.patterns.VariablePairPattern;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class CategoryTemplate
        extends templates.AbstractTemplate<Document, State, VariablePairPattern<Annotation>> {

    private static Logger log = LogManager.getFormatterLogger();
    private boolean useBins = false;
    final private static int NUMBER_OF_BINS = 1000;

    private static double[] bins = new double[NUMBER_OF_BINS + 1];

    static {

        for (int i = 0; i <= NUMBER_OF_BINS; i++) {
            bins[i] = (double) i / (double) NUMBER_OF_BINS;
        }

    }

    public CategoryTemplate(boolean b) {
        this.useBins = b;
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
        
        double sim = DBpediaEndpoint.normalizedCategorySimilarity(link1, link2);
        
        final int bin = getBin(sim);
        
        if (useBins) {
            for (int i = 0; i < bin; i++) {
                featureVector.set("PairwiseCategorySim_ >= " + i, sim);
            }

            featureVector.set("1PairwiseCategorySim_binInBin_" + bin, 1d);
            featureVector.set("ScorePairwiseCategorySim_binInBin_" + bin, sim);
        }
        else{
            featureVector.set("OneFeature_PairwiseCategorySimilary", sim);
        }
        
        
    }
    
    private int getBin(final double score) {
        for (int i = 0; i < bins.length - 1; i++) {
            if (bins[i] <= score && score < bins[i + 1]) {
                return i;
            }
        }
        return -1;
    }
    
}
