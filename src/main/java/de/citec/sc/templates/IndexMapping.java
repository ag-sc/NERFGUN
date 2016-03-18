/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sherzod
 */
public class IndexMapping {

    private static final int NUM_OF_GOLD_INDICIES = 18000000;
    public final static Map<String, Integer> indexMappings = new HashMap<>(NUM_OF_GOLD_INDICIES);
    public final static Map<Integer, String> indexMappingsInverse = new HashMap<>(NUM_OF_GOLD_INDICIES);

    public static void init(String keyFiles) {
        try {
            loadIndexMapping(keyFiles);
        } catch (IOException ex) {
            Logger.getLogger(IndexMapping.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void loadIndexMapping(final String keyFiles)
            throws FileNotFoundException, IOException {
        BufferedReader indexMappingReader = new BufferedReader(new FileReader(new File(keyFiles)));
        String line = "";
        while ((line = indexMappingReader.readLine()) != null) {
            String[] data = line.split("\t");
            final int nodeIndex = Integer.parseInt(data[0]);
            
            if(!((data[1].contains("Category:") || data[1].contains("File:") || data[1].contains("(disambiguation)")))){
                indexMappings.put(data[1], nodeIndex);
                indexMappingsInverse.put(nodeIndex, data[1]);
            }
            
        }
        indexMappingReader.close();
    }

}
