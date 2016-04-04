/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    public static Map<String, Integer> indexMappings;
    public static Map<Integer, String> indexMappingsInverse;

    public static void init(String keyFiles) {
        try {
            loadIndexMapping(keyFiles);
        } catch (IOException ex) {
            Logger.getLogger(IndexMapping.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void loadIndexMapping(final String keyFiles)
            throws FileNotFoundException, IOException {

        System.out.println("Loading index mapping ...");
        if (indexMappings == null && indexMappingsInverse == null) {
            indexMappings = new HashMap<>(NUM_OF_GOLD_INDICIES);
            indexMappingsInverse = new HashMap<>(NUM_OF_GOLD_INDICIES);

            FileInputStream fstream = new FileInputStream(keyFiles);
            BufferedReader indexMappingReader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));

            //BufferedReader indexMappingReader = new BufferedReader(new FileReader(new File(keyFiles)));
            String line = "";
            while ((line = indexMappingReader.readLine()) != null) {
                String[] data = line.split("\t");
                final int nodeIndex = Integer.parseInt(data[0]);

                if (!((data[1].contains("Category:") || data[1].contains("File:") || data[1].contains("(disambiguation)")))) {
                    indexMappings.put(data[1], nodeIndex);
                    indexMappingsInverse.put(nodeIndex, data[1]);
                }

            }
            indexMappingReader.close();

            
        }

    }

    private static void saveMap() {
        try {

            FileOutputStream fout1 = new FileOutputStream("serializedIndexMap.bin");
            FileOutputStream fout2 = new FileOutputStream("serializedInverseIndexMap.bin");
            ObjectOutputStream oos = new ObjectOutputStream(fout1);
            oos.writeObject(indexMappings);
            oos.close();
            System.out.println("Saved index map");

            oos = new ObjectOutputStream(fout2);
            oos.writeObject(indexMappingsInverse);
            oos.close();
            System.out.println("Saved inverse index map");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void readMap() {
        try {

            FileInputStream fin1 = new FileInputStream("serializedIndexMap.bin");
            ObjectInputStream ois = new ObjectInputStream(fin1);
            indexMappings = (Map) ois.readObject();
            ois.close();

            FileInputStream fin2 = new FileInputStream("serializedInverseIndexMap.bin");
            ois = new ObjectInputStream(fin2);
            indexMappingsInverse = (Map) ois.readObject();
            ois.close();

        } catch (Exception ex) {
            System.err.println("Files not found serializedIndexMap.bin and serializedInverseIndexMap.bin");
        }
    }

}
