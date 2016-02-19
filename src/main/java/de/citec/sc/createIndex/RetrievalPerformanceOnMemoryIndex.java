/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.query.Search;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author sherzod
 */
public class RetrievalPerformanceOnMemoryIndex {

    static long time;
    static String index;
    private static int topK;

    public static void main(String[] args) throws UnsupportedEncodingException {
        run();
    }

    public static void run() throws UnsupportedEncodingException {

        ConcurrentHashMap<String, HashMap<String, Integer>> surfaceForms = new ConcurrentHashMap<>(11000000);
        long start = System.currentTimeMillis();
        try (Stream<String> stream = Files.lines(Paths.get("dbpediaSurfaceForms.txt"))) {
            stream.parallel().forEach(item -> {

                String[] c = item.toString().split("\t");
                if (surfaceForms.containsKey(c[1])) {
                    int i = Integer.parseInt(c[2]);
                    HashMap<String, Integer> m = surfaceForms.get(c[1]);
                    m.put(c[0], i);

                    surfaceForms.put(c[1], m);
                } else {
                    int i = Integer.parseInt(c[2]);
                    HashMap<String, Integer> m = new HashMap<>();
                    m.put(c[0], i);
                    surfaceForms.put(c[1], m);
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        System.out.println("Loading: " + (end - start) + " ms");

        ConcurrentHashMap<String, HashMap<String, Integer>> anchorForms = new ConcurrentHashMap<>(11000000);
        start = System.currentTimeMillis();
        try (Stream<String> stream = Files.lines(Paths.get("anchorFiles/wikipedia_anchors.ttl"))) {
            //try (Stream<String> stream = Files.lines(Paths.get("testFiles/test.ttl"))) {
            stream.parallel().forEach(item -> {

                String[] c = item.toString().split("\t");

                if (c.length == 3) {
                    if (anchorForms.containsKey(c[0].toLowerCase())) {
                        int i = Integer.parseInt(c[2]);
                        HashMap<String, Integer> m = anchorForms.get(c[0].toLowerCase());
                        m.put(c[1].replace("http://dbpedia.org/resource/", ""), i);

                        anchorForms.put(c[0].toLowerCase(), m);
                    } else {
                        int i = Integer.parseInt(c[2]);
                        HashMap<String, Integer> m = new HashMap<>();
                        m.put(c[1].replace("http://dbpedia.org/resource/", ""), i);
                        anchorForms.put(c[0].toLowerCase(), m);
                    }
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        end = System.currentTimeMillis();
        System.out.println("Loading: " + (end - start) + " ms");

        List<Integer> topKs = new ArrayList<>();
//        topKs.add(10);
        topKs.add(2000);
//        topKs.add(1000);
//        topKs.add(2000);

        List<String> datasets = new ArrayList<>();
//        datasets.add("tweets");
        datasets.add("news");
//        datasets.add("small");

        List<String> indexType = new ArrayList<>();
//        indexType.add("dbpedia");
//        indexType.add("anchors");
        indexType.add("all");

        List<String> dbindexPaths = new ArrayList<>();
//        dbindexPaths.add("dbpediaIndexOnlyLabels");
//        dbindexPaths.add("dbpediaIndexOnlyOntology");
        dbindexPaths.add("dbpediaIndexAll");

        List<Boolean> useMemory = new ArrayList<>();
        useMemory.add(Boolean.FALSE);
//        useMemory.add(Boolean.TRUE);

        CorpusLoader loader = new CorpusLoader(false);

        String overallResult = "";
        
        

        for (String indexT : indexType) {
            for (Integer t = 350; t <= 350; t = t + 50) {

                for (String dataset : datasets) {
                    for (String dbpediaIndexPath : dbindexPaths) {

                        topK = t;
                        time = 0;
                        index = indexT;

                        DefaultCorpus c = new DefaultCorpus();

                        //set the dataset
                        if (dataset.equals("tweets")) {
                            c = loader.loadCorpus(CorpusLoader.CorpusName.MicroTagging);
                        }
                        if (dataset.equals("news")) {
                            c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLL);
                        }
                        if (dataset.equals("small")) {
                            c = loader.loadCorpus(CorpusLoader.CorpusName.SmallCorpus);
                        }

                        HashMap<String, Set<String>> notFound = new HashMap<String, Set<String>>();

                        System.out.println(c.getDocuments().size());

                        List<Document> docs = c.getDocuments();

                        int annotationsCount = 0;

                        for (Document d : docs) {

                            List<Annotation> annotations = d.getGoldStandard();

                            annotationsCount += annotations.size();

                            for (Annotation a : annotations) {

                                start = System.nanoTime();
                                HashMap<String, Integer> result1 = anchorForms.get(a.getWord().toLowerCase());
                                HashMap<String, Integer> result2 = surfaceForms.get(a.getWord().toLowerCase());
                                end = System.nanoTime();
                                time += (end - start);

                                result1 = sortByValue(result1);
                                result2 = sortByValue(result2);

                                HashMap<String, Integer> result = new LinkedHashMap<>();
                                int counter = 0;
                                for (String s1 : result1.keySet()) {
                                    if (counter <= t) {
                                        result.put(s1, result1.get(s1));
                                        counter++;
                                    } else {
                                        break;
                                    }
                                }
                                counter = 0;
                                for (String s1 : result2.keySet()) {
                                    if (counter <= t) {
                                        result.put(s1, result2.get(s1));
                                        counter++;
                                    } else {
                                        break;
                                    }
                                }
                                
                                
                                
                                

                                //if the link in annoation is redirect page
                                //replace it with the original one
                                //decoder the URI with UTF-8 encoding
                                String link = a.getLink();

                                link = link.replace("http://en.wikipedia.org/wiki/", "");
                                link = URLDecoder.decode(link, "UTF-8");

                                a.setLink(link);

                                if (result.containsKey(link)) {
                                    d.addAnnotation(a.clone());
                                } else {

                                    if (notFound.containsKey(a.getWord())) {
                                        Set<String> list = notFound.get(a.getWord());

                                        list.add(link);

                                        notFound.put(a.getWord(), list);
                                    } else {
                                        Set<String> list = new HashSet<>();

                                        list.add(link);

                                        notFound.put(a.getWord(), list);
                                    }

                                }

//                                if (result.containsKey(link)) {
//                                    int indexOfKey = 0;
//                                    for (String r1 : result.keySet()) {
//                                        if (!r1.equals(link)) {
//                                            indexOfKey++;
//                                        } else {
//                                            if (indexOfKey < t) {
//
//                                            }
//                                        }
//                                    }
//                                }
                            }

                            System.out.println(docs.indexOf(d) + "  " + topK);
//                                System.out.println(d);
                        }

                        Evaluator eva = new Evaluator();

                        Map<String, Double> result = eva.evaluateAll(docs);

                        String s1 = "";
                        for (String s : result.keySet()) {
                            s1 += s + " " + result.get(s) + "\n";
                            System.out.println(s + " " + result.get(s));
                        }

                        time = time / (long) annotationsCount;
                        s1 += "\n\nRuntime per entity: " + time + " nanoseconds.";

                        String n = "";
                        for (String n1 : notFound.keySet()) {
                            n += n1;
                            for (String l : notFound.get(n1)) {
                                n += "\t" + l;
                            }
                            n += "\n";
                        }

                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();

                        String stamp = dateFormat.format(date).replace(" ", "_");

//                        writeListToFile("retrieval/notFound_memory_" + m + "_top_" + topK + "_" + dataset + "_index_" + index + "_property_" + dbpediaIndexPath + ".txt", n);
//                        writeListToFile("retrieval/results_memory_" + m + "_top_" + topK + "_" + dataset + "_index_" + index + "_property_" + dbpediaIndexPath + ".txt", s1);
                        overallResult += "top_" + topK + "_" + dataset + "_index_" + index + "_property_" + dbpediaIndexPath + "" + "\n\n" + s1 + "\n\n\n";
                    }
                }
            }
        }
       

        writeListToFile("overallResultFirstElementAnc.txt", overallResult);
    }

    private static HashMap<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        if (unsortMap == null) {
            return new HashMap<>();
        }

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2) {

                return o2.getValue().compareTo(o1.getValue());

            }
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void writeListToFile(String fileName, String content) {
        try {
            File file = new File(fileName);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean compareToHighest(String link, HashMap<String, Integer> map) {
        int max = 0;
        String candidate = "";
        if (map == null) {
            return false;
        }
        for (String c : map.keySet()) {
            if (max < map.get(c)) {
                max = map.get(c);
                try {
                    candidate = URLDecoder.decode(c, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(RetrievalPerformanceOnMemoryIndex.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (link.equals(candidate)) {
            return true;
        }
        return false;
    }

}
