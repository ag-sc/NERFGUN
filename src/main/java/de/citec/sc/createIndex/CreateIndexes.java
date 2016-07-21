/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import de.citec.sc.index.AnchorTextLoader;
import de.citec.sc.index.SurfaceFormsDBpedia;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.citec.sc.index.ProcessAnchorFile;

/**
 *
 * @author sherzod
 */
public class CreateIndexes {

    public static void main(String[] args) {
        run();
    }

    public static void run() {

//        SurfaceFormsDBpedia dbpediaLoader = new SurfaceFormsDBpedia("propList3.txt");
//
//        dbpediaLoader.load("dbpediaFiles/");
        
//        try {
//            System.out.println("cleaning anchor file ...");
//            ProcessAnchorFile.run("anchorFiles/wikipedia_anchors.ttl");
//            System.out.println("cleaning dbpedia file ...");
//            ProcessAnchorFile.run("dbpediaFiles/dbpediaSurfaceForms.ttl");
//
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(CreateIndexes.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(CreateIndexes.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
        System.out.println("Creating index files ...");
        AnchorTextLoader anchorLoader = new AnchorTextLoader();
        anchorLoader.load(true, "mergedIndexNew", "anchorFiles/");

//        anchorLoader.load(true, "emptyIndex", "testFiles/");
//
        System.out.println("DONE.");
    }
}
