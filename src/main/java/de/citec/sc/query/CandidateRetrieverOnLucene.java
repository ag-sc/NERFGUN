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

    private AnchorRetriever dbpediaRetriever;

    private boolean loadToMemory;

    public CandidateRetrieverOnLucene(boolean loadToMemory, String dbpediaIndexPath) {
        this.loadToMemory = loadToMemory;

        this.dbpediaRetriever = new AnchorRetriever(dbpediaIndexPath, loadToMemory);
        

    }

    @Override
    public List<Instance> getAllResources(String searchTerm, int topK) {
        return dbpediaRetriever.getResources(searchTerm, topK);
    }

//    @Override
//    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK) {
//        return anchorRetriever.getResources(searchTerm, topK);
//    }
//
//    @Override
//    public List<Instance> getAllResources(String searchTerm, int topK) {
//        List<Instance> result = new ArrayList<>();
//        
//        if(searchTerm.equals(""))
//            return result;
//
//        List<Instance> anchor = anchorRetriever.getResources(searchTerm, topK);
//        List<Instance> dbpedia = dbpediaRetriever.getResources(searchTerm, topK);
//
//        if (anchor.size() < dbpedia.size()) {
//            int a = Math.min(topK / 2, anchor.size());
//            int b = topK - a;
//            result.addAll(anchor.subList(0, a));
//            result.addAll(dbpedia.subList(0, Math.min(b, dbpedia.size())));
//        } else {
//            int a = Math.min(topK / 2, dbpedia.size());
//            int b = topK - a;
//            result.addAll(anchor.subList(0, Math.min(b, anchor.size())));
//            result.addAll(dbpedia.subList(0, a));
//        }
//        return result;
//    }

    @Override
    public List<Instance> getResourcesFromDBpedia(String searchTerm, int topK) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
