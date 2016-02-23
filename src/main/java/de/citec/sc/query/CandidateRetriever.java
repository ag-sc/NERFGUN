/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.query;

import java.util.List;

/**
 *
 * @author sherzod
 */
public interface CandidateRetriever {
    public List<Instance> getResourcesFromDBpedia(String searchTerm, int topK);
    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK);
    public List<Instance> getAllResources(String searchTerm, int topK);
}
