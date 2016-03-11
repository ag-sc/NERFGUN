/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import de.citec.sc.BIREMain;
import de.citec.sc.BIRETestModelsMain;
import java.util.Arrays;

/**
 *
 * @author sherzod
 */
public class Main {

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        //TestOnMemory.main(args);

        long start = System.currentTimeMillis();
//        CandidateRetrieverOnMemory indexQuery = new CandidateRetrieverOnMemory();
//        System.out.println(indexQuery.getAllResources("obama", 100));
//        RetrievalPerformanceOnMemoryIndex.run();
//        CreateIndexes.run();
//        RetrievalPerformance.run();
//        RetrievalPerformancePageRank.run();

        
        if(args[2].equals("test")){
            try
            {
                BIRETestModelsMain.main(args);
            }
            catch(Exception e){
            
            }
        }
        
        if(args[2].equals("train")){
            try
            {
                BIREMain.main(args);
            }
            catch(Exception e){
            
            }
        }
        
//        TestSearch.main(args);

        long end = System.currentTimeMillis();
        int mb = 1024 * 1024;

        // Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        System.out.println("##### Heap utilization statistics [MB] #####");

        // Print used memory
        System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        // Print free memory
        System.out.println("Free Memory:" + runtime.freeMemory() / mb);

        // Print total available memory
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);

    }
}
