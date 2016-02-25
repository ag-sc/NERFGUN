/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.createIndex;

import de.citec.sc.query.CandidateRetrieverOnMemory;
import java.io.UnsupportedEncodingException;
import test.TestOnMemory;

/**
 *
 * @author sherzod
 */
public class Main {
    public static void main(String[] args) throws UnsupportedEncodingException{
        //TestOnMemory.main(args);
        
//        CandidateRetrieverOnMemory indexQuery = new CandidateRetrieverOnMemory();
//        System.out.println(indexQuery.getAllResources("obama", 100));
        
//        RetrievalPerformanceOnMemoryIndex.run();
        
        CreateIndexes.run();
        
//        RetrievalPerformance.run();
    }
}
