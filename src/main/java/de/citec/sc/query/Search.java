/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sherzod
 */
public class Search {


    private DBpediaRetriever dbpediaRetriever;
    private AnchorRetriever anchorRetriever;


    private boolean loadToMemory;

    public Search(boolean loadToMemory, String dbpediaIndexPath) {
        this.loadToMemory = loadToMemory;

        
        this.dbpediaRetriever = new DBpediaRetriever(dbpediaIndexPath, loadToMemory);
        this.anchorRetriever = new AnchorRetriever("anchorIndex", loadToMemory);
        
    }

    /**
     * @return returns all resources that match the label using DBpedia
     * @param searchTerm
     * @param topK
     */
    public Set<String> getResourcesFromDBpedia(String searchTerm, int topK) {
        Set<String> result = new LinkedHashSet<>();

        Set<String> searchTermSet = new LinkedHashSet<>();
        searchTermSet.add(searchTerm);

        for (String term : searchTermSet) {
            //get all from DBpedia
            for (String e1 : dbpediaRetriever.getResources(term, topK, false)) {
//                try {
                result.add(e1);
//                    result.add(URLDecoder.decode(e1, "UTF-8"));
//                } catch (UnsupportedEncodingException ex) {
//                    Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
//                }
            }
        }
        
        if(result.size() > topK){
            List<String> temp = new ArrayList<>();
            temp.addAll(result);
            
            result.clear();
            
            result.addAll(temp.subList(0, topK));
        }

        return result;
    }

    /**
     * @return returns all resources that match the label using DBpedia
     * @param searchTerm
     * @param topK
     */
    public Set<String> getResourcesFromAnchors(String searchTerm, int topK) {
        Set<String> result = new LinkedHashSet<>();

        Set<String> searchTermSet = new LinkedHashSet<>();
        searchTermSet.add(searchTerm);

        for (String term : searchTermSet) {
            //get all from Anchor
            for (String e1 : anchorRetriever.getResources(term, topK)) {
                try {

                    result.add(URLDecoder.decode(e1, "UTF-8"));

                    int z = 1;
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        if(result.size() > topK){
            List<String> temp = new ArrayList<>();
            temp.addAll(result);
            
            result.clear();
            
            result.addAll(temp.subList(0, topK));
        }

        return result;
    }

    /**
     * @return returns all resources that match the label using DBpedia and
     * Anchors
     * @param searchTerm
     * @param topK
     */
    public Set<String> getAllResources(String searchTerm, int topK) {
        Set<String> result = new LinkedHashSet<>();

        Set<String> searchTermSet = new LinkedHashSet<>();
        searchTermSet.add(searchTerm);


        for (String term : searchTermSet) {
            for (String e1 : anchorRetriever.getResources(term, topK / 2)) {

                result.add(e1);

            }
            //add resources from DBpedia
            for (String e1 : dbpediaRetriever.getResources(term, topK / 2, false)) {
                result.add(e1);
            }
        }
        
        if(result.size() > topK){
            List<String> temp = new ArrayList<>();
            temp.addAll(result);
            
            result.clear();
            
            result.addAll(temp.subList(0, topK));
        }
        return result;
    }
}
