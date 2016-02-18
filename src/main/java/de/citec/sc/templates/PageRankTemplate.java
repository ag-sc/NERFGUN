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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import learning.Vector;
import utility.VariableID;

/**
 *
 * @author sherzod
 */
public class PageRankTemplate extends templates.AbstractTemplate<State> {

    private HashMap<String, Double> pageRankMap;

    public PageRankTemplate() {
        loadPageRanks();
    }

    @Override
    protected Collection<AbstractFactor> generateFactors(State state) {
        Set<AbstractFactor> factors = new HashSet<>();
        for (VariableID entityID : state.getEntityIDs()) {
            factors.add(new SingleVariableFactor(this, entityID));
        }
        return factors;
    }

    @Override
    protected void computeFactor(State state, AbstractFactor absFactor) {
        if (absFactor instanceof SingleVariableFactor) {

            SingleVariableFactor factor = (SingleVariableFactor) absFactor;
            Annotation entity = state.getEntity(factor.entityID);
            String uri = entity.getLink().replace("http://dbpedia.org/resource/", "");
            Double rank = pageRankMap.get(uri);

            Vector featureVector = new Vector();

            String pageRankPrefix = "PageRank=";

            featureVector.set(pageRankPrefix, rank);

            featureVector.set("PageRank_HIGHER_THAN_100", rank > 100 ? 1.0 : 0);

            factor.setFeatures(featureVector);
        }
    }

    private void loadPageRanks() {
        this.pageRankMap = new HashMap<>();
        String path = "dbpediaFiles/pageranks.ttl";
        String patternString = "<http://dbpedia.org/resource/(.*?)>.*\"(.*?)\"";
        Pattern pattern1 = Pattern.compile(patternString);

        try (Stream<String> stream = Files.lines(Paths.get(path))) {

            stream.parallel().forEach(item -> {

                String line = item.toString();

                Matcher m = pattern1.matcher(line);
                while (m.find()) {
                    String uri = m.group(1);

                    String r = m.group(2);
                    Double v = Double.parseDouble(r);
                    if (v > 1.0) {
                        if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
                            pageRankMap.put(uri, v);
                        }
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
