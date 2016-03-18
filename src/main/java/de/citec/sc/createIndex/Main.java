/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import de.citec.sc.BIREMain;
import de.citec.sc.BIRETestModelsMain;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnLucene;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.sampling.AllScoresExplorer;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author sherzod
 */
public class Main {

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        
//        RetrievalPerformance.run();
//        RetrievalPerformancePageRank.run();
        if (args[2].equals("test")) {
            try {
                BIRETestModelsMain.main(args);
            } catch (Exception e) {

            }
        }

        if (args[2].equals("train")) {
            try {
                BIREMain.main(args);
            } catch (Exception e) {

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
