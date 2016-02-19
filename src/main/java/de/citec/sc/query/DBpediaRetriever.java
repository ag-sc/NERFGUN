/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author sherzod
 */
public class DBpediaRetriever extends LabelRetriever {

    
    private String instancesTokenizedIndexPath = "resourceTokenizedIndex";
    private String instancesIndexPath = "resourceIndex";
    private String directory;
    private StandardAnalyzer analyzer;

    private Directory instanceTokenizedIndexDirectory;
    private Directory instanceIndexDirectory;

    public DBpediaRetriever(String directory, boolean loadIntoMemory) {
        this.directory = directory;
        initIndexDirectory(loadIntoMemory);

    }

    private void initIndexDirectory(boolean loadToMemory) {
        try {
            String instancesTokenizedPath = directory + "/" + this.instancesTokenizedIndexPath + "/";
            String instancePath = directory + "/" + this.instancesIndexPath + "/";

            analyzer = new StandardAnalyzer();
            if (loadToMemory) {
                instanceTokenizedIndexDirectory = new RAMDirectory(FSDirectory.open(Paths.get(instancesTokenizedPath)), IOContext.DEFAULT);
                instanceIndexDirectory = new RAMDirectory(FSDirectory.open(Paths.get(instancePath)), IOContext.DEFAULT);
            } else {
                instanceTokenizedIndexDirectory = FSDirectory.open(Paths.get(instancesTokenizedPath));
                instanceIndexDirectory = FSDirectory.open(Paths.get(instancePath));
            }

        } catch (Exception e) {
            System.err.println("Problem with initializing InstanceQueryProcessor\n" + e.getMessage());
        }
    }

    /**
     * return resources with namespace http://dbpedia.org/resource/
     * if mergePartialMatches is set to true then results would be direct matches + partial matches
     * else only direct matches from index
     * 
     * @param searchTerm
     * @param k
     * @param mergePartialMatches
     * 
     * 
     */
    public Set<String> getResources(String searchTerm, int k, boolean mergePartialMatches) {

        super.comparator = super.frequencyComparator;

        Set<Instance> resultDirectMatch = getDirectMatches(searchTerm, "label", "URI", k, instanceIndexDirectory);
        

        if (mergePartialMatches) {
            Set<Instance> resultPartialMatch = getPartialMatches(searchTerm, "labelTokenized", "URI", k, instanceTokenizedIndexDirectory, analyzer);
            //add partial matches to direct
            resultDirectMatch.addAll(resultPartialMatch);
        }

        Set<String> resources = new LinkedHashSet<>();
        resultDirectMatch.forEach(i1 -> resources.add(i1.getUri()));

        //resources = sortBySimilarity(resources, searchTerm, k);
        return resources;

    }

}
