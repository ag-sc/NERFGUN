/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class CandidateRetrieverOnLucene implements CandidateRetriever {

    private DBpediaRetriever dbpediaRetriever;
    private AnchorRetriever anchorRetriever;

    private boolean loadToMemory;

    public CandidateRetrieverOnLucene(boolean loadToMemory, String dbpediaIndexPath, String anchorIndexPath) {
        this.loadToMemory = loadToMemory;

        this.dbpediaRetriever = new DBpediaRetriever(dbpediaIndexPath, loadToMemory);
        this.anchorRetriever = new AnchorRetriever(anchorIndexPath, loadToMemory);

    }

    @Override
    public List<Instance> getResourcesFromDBpedia(String searchTerm, int topK) {
        return dbpediaRetriever.getResources(searchTerm, topK, false);
    }

    @Override
    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK) {
        return anchorRetriever.getResources(searchTerm, topK);
    }

    @Override
    public List<Instance> getAllResources(String searchTerm, int topK) {
        List<Instance> result = new ArrayList<>();

        List<Instance> anchor = anchorRetriever.getResources(searchTerm, topK);
        List<Instance> dbpedia = dbpediaRetriever.getResources(searchTerm, topK, false);

        if (anchor.size() < dbpedia.size()) {
            int a = Math.min(topK / 2, anchor.size());
            int b = topK - a;
            result.addAll(anchor.subList(0, a));
            result.addAll(dbpedia.subList(0, Math.min(b, dbpedia.size())));
        } else {
            int a = Math.min(topK / 2, dbpedia.size());
            int b = topK - a;
            result.addAll(anchor.subList(0, Math.min(b, anchor.size())));
            result.addAll(dbpedia.subList(0, a));
        }
        return result;
    }

}