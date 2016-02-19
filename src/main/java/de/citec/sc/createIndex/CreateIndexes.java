/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import de.citec.sc.index.AnchorTextLoader;
import de.citec.sc.index.DBpediaLoader;
import de.citec.sc.index.SurfaceFormsDBpedia;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class CreateIndexes {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("Creating index files ...");

        DBpediaLoader dbpediaLoader = new DBpediaLoader("propList1.txt");
//
//        System.out.println("dbpediaIndexOnlyLabels ...");
//        dbpediaLoader.load(true, "dbpediaIndexOnlyLabels", "dbpediaFiles/");
//
        dbpediaLoader = new DBpediaLoader("propList3.txt");
        System.out.println("dbpediaIndex ...");
        dbpediaLoader.load(true, "dbpediaIndex", "testFiles/");
//
        
//        SurfaceFormsDBpedia dbpediaLoader = new SurfaceFormsDBpedia("propList3.txt");
//        System.out.println("dbpediaIndexAll ...");
//        dbpediaLoader.load("dbpediaFiles/");

//        AnchorTextLoader anchorLoader = new AnchorTextLoader();
//        anchorLoader.load(true, "anchorIndex", "anchorFiles/");

        System.out.println("DONE.");
    }
}
