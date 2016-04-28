/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class CandidateRetrieverOnMemory implements CandidateRetriever {

    private Map<String, HashMap<String, Integer>> dbpediSurfaceForms;
    private boolean isInitialized = false;

    public CandidateRetrieverOnMemory() {
        if (!isInitialized) {
            System.out.println("Loading dbpedia surface forms ...");
            loadFiles("anchorFiles/");
        }

    }

    public CandidateRetrieverOnMemory(String path) {

        
        System.out.println("Loading dbpedia surface forms ...");
        loadFiles(path + "/");

    }

    @Override
    public List<Instance> getAllResources(String searchTerm, int topK) {
        HashMap<String, Integer> result1 = dbpediSurfaceForms.get(searchTerm.toLowerCase());

        result1 = sortByValue(result1);

        HashMap<String, Integer> result = new LinkedHashMap<>();
        int counter = 0;
        
        for (String s1 : result1.keySet()) {
            if (counter < topK) {
                result.put(s1, result1.get(s1));
                counter++;
            } else {
                break;
            }
        }

        int sum = 0;
        for (Integer i : result.values()) {
            sum += i;
        }
        List<Instance> instances = new ArrayList<>();
        for (String r1 : result.keySet()) {
            double score = (double) result.get(r1) / (double) sum;
            Instance i = new Instance(r1, result.get(r1));
            i.setScore(score);
//            i.setScore(result.get(r1));
            instances.add(i);
        }

        return instances;
    }

    private void loadFiles(String directory) {

        if (this.dbpediSurfaceForms == null) {

            dbpediSurfaceForms = new ConcurrentHashMap<>(18000000);

            File indexFolder = new File(directory);
            File[] listOfFiles = indexFolder.listFiles();

            for (int d = 0; d < listOfFiles.length; d++) {
                if (listOfFiles[d].isFile() && !listOfFiles[d].isHidden()) {

                    String fileExtension = listOfFiles[d].getName().substring(listOfFiles[d].getName().lastIndexOf(".") + 1);

                    if (fileExtension.equals("ttl")) {

                        String filePath = listOfFiles[d].getPath();

                        System.out.println("Loading " + filePath);

                        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
                            stream.parallel().forEach(item -> {

                                String[] c = item.toString().split("\t");
                                if (c.length == 3) {

                                    String label = c[0].toLowerCase();
                                    String uri = c[1];
                                    uri = uri.replace("http://dbpedia.org/resource/", "");
                                    int freq = Integer.parseInt(c[2]);

                                    if (!(uri.contains("Category:") || uri.contains("(disambiguation)") || uri.contains("File:"))) {
                                        if (dbpediSurfaceForms.containsKey(label)) {

                                            HashMap<String, Integer> m = dbpediSurfaceForms.get(label);
                                            if (m.containsKey(uri)) {
                                                m.put(uri, freq + m.get(uri));
                                            } else {
                                                m.put(uri, freq);
                                            }

                                            dbpediSurfaceForms.put(label, m);
                                        } else {

                                            HashMap<String, Integer> m = new HashMap<>();
                                            m.put(uri, freq);
                                            dbpediSurfaceForms.put(label, m);
                                        }
                                    }
                                }

                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        isInitialized = true;
    }

    private HashMap<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        if (unsortMap == null) {
            return new HashMap<>();
        }

        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                    Map.Entry<String, Integer> o2) {

                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    @Override
    public List<Instance> getResourcesFromDBpedia(String searchTerm, int topK) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
