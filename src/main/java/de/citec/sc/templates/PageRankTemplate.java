/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.variables.State;
import factors.AbstractFactor;
import factors.impl.SingleVariableFactor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import learning.Vector;
import org.apache.logging.log4j.LogManager;
import test.TestSearch;
import utility.VariableID;

/**
 *
 * @author sherzod
 */
public class PageRankTemplate extends templates.AbstractTemplate<State> {

    private static org.apache.logging.log4j.Logger log = LogManager.getFormatterLogger();

    ConcurrentHashMap<String, Double> pageRankMap;

    public PageRankTemplate() {
        loadPageRanks();
    }

    @Override
    protected Collection<AbstractFactor> generateFactors(State state) {
        Set<AbstractFactor> factors = new HashSet<>();
        for (VariableID entityID : state.getEntityIDs()) {
            factors.add(new SingleVariableFactor(this, entityID));
        }
        log.info("Generate %s factors for state %s.", factors.size(), state.getID());
        return factors;
    }

    @Override
    protected void computeFactor(State state, AbstractFactor absFactor) {
        if (absFactor instanceof SingleVariableFactor) {

            SingleVariableFactor factor = (SingleVariableFactor) absFactor;
            Annotation entity = state.getEntity(factor.entityID);
            String uri = entity.getLink();
            Double rank = pageRankMap.get(uri);
            if (rank == null) {
                rank = 0.0;
            }

            Vector featureVector = new Vector();

            String pageRankPrefix = "PageRank";

            featureVector.set(pageRankPrefix, rank);

            //featureVector.set("PageRank_HIGHER_THAN_100", rank > 100 ? 1.0 : 0);
            factor.setFeatures(featureVector);
        }
    }

    private void loadPageRanks() {
        
        if (pageRankMap == null) {
            
            pageRankMap = new ConcurrentHashMap<>(19500000);
            String path = "dbpediaFiles/pageranks.ttl";
            String patternString = "<http://dbpedia.org/resource/(.*?)>.*\"(.*?)\"";
            Pattern pattern1 = Pattern.compile(patternString);

            System.out.print("Loading pagerank scores to memory ...");

            try (Stream<String> stream = Files.lines(Paths.get(path))) {
                stream.parallel().forEach(item -> {

                    String line = item.toString();

                    Matcher m = pattern1.matcher(line);
                    while (m.find()) {
                        String uri = m.group(1);

                        String r = m.group(2);
                        Double v = Double.parseDouble(r);

                        if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
                            try {
                                //counter.incrementAndGet();
                                uri = URLDecoder.decode(uri, "UTF-8");
                            } catch (UnsupportedEncodingException ex) {
                                Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            pageRankMap.put(uri, v);

                        }

                    }

                });

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("  DONE");
        }

    }

}
