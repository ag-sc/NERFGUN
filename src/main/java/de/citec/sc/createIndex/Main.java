/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import de.citec.sc.BIRETest;
import de.citec.sc.BIRETrain;
import de.citec.sc.SVMTest;
import de.citec.sc.SVMTrain;

/**
 *
 * @author sherzod
 */
public class Main {

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";
    private static final String PARAM_RUN = "-r";

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {

//        args = new String[4];
//        args[0] = "mdeol";
//        args[1] = "CoNLLTesta";
//        args[2] = "-r";
//        args[3] = "test";
//////        args[4] = "-n";
//////        args[5] = "1";
//////        args[6] = "-d";
//////        args[7] = "CoNLLTraining";
////
//        args[0] = "-s";
//        args[1] = "0";
//        args[2] = "-r";
//        args[3] = "wekaTrain";
//
//        args[4] = "-n";
//        args[5] = "1";
//        
//        args[6] = "-d";
//        args[7] = "CoNLLTraining";
        
        readParamsFromCommandLine(args);

//        RetrievalPerformance.run();
//        RetrievalPerformancePageRank.run();
        if (PARAMETERS.get(PARAM_RUN).equals("test")) {
            try {
                BIRETest.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (PARAMETERS.get(PARAM_RUN).equals("train")) {
            try {
                BIRETrain.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (PARAMETERS.get(PARAM_RUN).equals("wekaTrain")) {
            try {
                SVMTrain.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (PARAMETERS.get(PARAM_RUN).equals("wekaTest")) {
            try {
                SVMTest.main(args);
            } catch (Exception e) {
                e.printStackTrace();
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

    private static void readParamsFromCommandLine(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith(PARAMETER_PREFIX)) {
                    PARAMETERS.put(args[i], args[i++ + 1]); // Skip value
                }
            }
        }
    }
}
