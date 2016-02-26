/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import de.citec.sc.BIREMain;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author sherzod
 */
public class Main {

    public static void main(String[] args) throws UnsupportedEncodingException {
        //TestOnMemory.main(args);

//        CandidateRetrieverOnMemory indexQuery = new CandidateRetrieverOnMemory();
//        System.out.println(indexQuery.getAllResources("obama", 100));
//        RetrievalPerformanceOnMemoryIndex.run();
//        CreateIndexes.run();
//        RetrievalPerformance.run();
//        RetrievalPerformancePageRank.run();
        BIREMain.main(args);

        int mb = 1024 * 1024;

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        System.out.println("##### Heap utilization statistics [MB] #####");

        //Print used memory
        System.out.println("Used Memory:"
                + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        //Print free memory
        System.out.println("Free Memory:"
                + runtime.freeMemory() / mb);

        //Print total available memory
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);

        //Print Maximum available memory
        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }
}
