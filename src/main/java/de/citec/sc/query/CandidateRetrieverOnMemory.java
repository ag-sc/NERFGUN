/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.io.IOException;
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

    private ConcurrentHashMap<String, HashMap<String, Integer>> dbpediSurfaceForms;
    private ConcurrentHashMap<String, HashMap<String, Integer>> anchorSurfaceForms;

    public CandidateRetrieverOnMemory() {
        System.out.println("Loading dbpedia surface forms ...");
        dbpediSurfaceForms = new ConcurrentHashMap<>();
        loadFiles("dbpediaFiles/dbpediaSurfaceForms.ttl", dbpediSurfaceForms);
        System.out.println("Loading anchor surface forms ...");
        System.out.println(dbpediSurfaceForms.size());
        anchorSurfaceForms = new ConcurrentHashMap<>();
        loadFiles("anchorFiles/wikipedia_anchors.ttl", anchorSurfaceForms);
    }

    @Override
    public List<Instance> getResourcesFromDBpedia(String searchTerm, int topK) {
        HashMap<String, Integer> result1 = dbpediSurfaceForms.get(searchTerm.toLowerCase());

        result1 = sortByValue(result1);

        HashMap<String, Integer> result = new LinkedHashMap<>();
        int counter = 0;
        for (String s1 : result1.keySet()) {
            if (counter <= topK) {
                try {
                    s1 = URLDecoder.decode(s1, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(CandidateRetrieverOnMemory.class.getName()).log(Level.SEVERE, null, ex);
                }
                result.put(s1, result1.get(s1));
                counter++;
            } else {
                break;
            }
        }

       //        int sum = 0;
//        for (Integer i : result.values()) {
//            sum += i;
//        }
        List<Instance> instances = new ArrayList<>();
        for (String r1 : result.keySet()) {
            //double score = (double) result.get(r1) / (double) sum;
            Instance i = new Instance(r1, result.get(r1));
            instances.add(i);
        }

        return instances;
    }

    @Override
    public List<Instance> getResourcesFromAnchors(String searchTerm, int topK) {

        HashMap<String, Integer> result2 = anchorSurfaceForms.get(searchTerm.toLowerCase());

        result2 = sortByValue(result2);

        HashMap<String, Integer> result = new LinkedHashMap<>();
        int counter = 0;

        for (String s1 : result2.keySet()) {
            if (counter <= topK) {
                try {
                    s1 = URLDecoder.decode(s1, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(CandidateRetrieverOnMemory.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (result.containsKey(s1)) {
                    result.put(s1, result.get(s1) + result2.get(s1));
                } else {
                    result.put(s1, result2.get(s1));
                }
                counter++;
            } else {
                break;
            }
        }

//        int sum = 0;
//        for (Integer i : result.values()) {
//            sum += i;
//        }
        List<Instance> instances = new ArrayList<>();
        for (String r1 : result.keySet()) {
            //double score = (double) result.get(r1) / (double) sum;
            Instance i = new Instance(r1, result.get(r1));
            instances.add(i);
        }

        return instances;
    }

    @Override
    public List<Instance> getAllResources(String searchTerm, int topK) {
        HashMap<String, Integer> result1 = dbpediSurfaceForms.get(searchTerm.toLowerCase());
        HashMap<String, Integer> result2 = anchorSurfaceForms.get(searchTerm.toLowerCase());

        result1 = sortByValue(result1);
        result2 = sortByValue(result2);
        List<Instance> instances = new ArrayList<>();

        try {
            HashMap<String, Integer> result = new LinkedHashMap<>();
            int counter = 0;
            for (String s1 : result1.keySet()) {
                if (counter <= topK) {
                    try {
                        s1 = URLDecoder.decode(s1, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(CandidateRetrieverOnMemory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    result.put(s1, result1.get(s1));
                    counter++;
                } else {
                    break;
                }
            }
            counter = 0;
            for (String s1 : result2.keySet()) {
                if (counter <= topK) {
                    try {
                        s1 = URLDecoder.decode(s1, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(CandidateRetrieverOnMemory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    result.put(s1, result2.get(s1));
                    counter++;
                } else {
                    break;
                }
            }

//            int sum = 0;
//            for (Integer i : result.values()) {
//                sum += i;
//            }
            for (String r1 : result.keySet()) {
                //double score = (double) result.get(r1) / (double) sum;
                Instance i = new Instance(r1, result.get(r1));
                instances.add(i);
            }
        } catch (Exception e) {

        }

        return instances;
    }

    private void loadFiles(String filePath, ConcurrentHashMap<String, HashMap<String, Integer>> map) {

        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.parallel().forEach(item -> {

                String[] c = item.toString().split("\t");
                if (c.length == 3) {
                    if (map.containsKey(c[0].toLowerCase())) {
                        int i = Integer.parseInt(c[2]);
                        HashMap<String, Integer> m = map.get(c[0].toLowerCase());
                        m.put(c[1].replace("http://dbpedia.org/resource/", ""), i);

                        map.put(c[0].toLowerCase(), m);
                    } else {
                        int i = Integer.parseInt(c[2]);
                        HashMap<String, Integer> m = new HashMap<>();
                        m.put(c[1].replace("http://dbpedia.org/resource/", ""), i);
                        map.put(c[0].toLowerCase(), m);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

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
}
