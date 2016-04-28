/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//github.com/ag-sc/NED.git
import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.query.Instance;
import de.citec.sc.templates.IndexMapping;
import java.util.LinkedHashMap;

/**
 *
 * @author sherzod
 */
public class RetrievalPerformance {

    static long time;
    private static int topK;

    public static void run(String[] args) throws UnsupportedEncodingException {

        List<String> datasets = new ArrayList<>();
//        datasets.add("CoNLLTesta");
//        datasets.add("CoNLLTestb");
        datasets.add("CoNLLTraining");
        datasets.add("MicroTag2014Train");

        boolean compareAll = true;

        CorpusLoader loader = new CorpusLoader(false);

        // indexSearch = new CandidateRetrieverOnLucene(m, "mergedIndex");
        System.out.println("Loading index files ... ");
        long start = System.currentTimeMillis();

        List<String> approaches = new ArrayList<String>();
        approaches.add("DBpedia");
        approaches.add("Wikipedia");
        approaches.add("Merged");

        HashMap<String, List<Double>> results = new LinkedHashMap<>();

        for (String approachPath : approaches) {

            CandidateRetrieverOnMemory indexSearch = new CandidateRetrieverOnMemory(approachPath);
            long end = System.currentTimeMillis();

            System.out.println("Loaded in " + (end - start) + " ms.");

            for (String dataset : datasets) {

                List<Double> values = new ArrayList<>();

                for (int t = 1; t <= 100; t = t + 1) {

                    if (t >= 11) {
                        t += 9;
                    }

                    topK = t;
                    time = 0;

                    DefaultCorpus c = loader.loadCorpus(CorpusLoader.CorpusName.valueOf(dataset));

                    HashMap<String, Set<String>> notFound = new HashMap<String, Set<String>>();

                    List<Document> docs = c.getDocuments();

                    int annotationsCount = 0;

                    for (Document d : docs) {

                        List<Annotation> annotations = d.getGoldStandard();

                        annotationsCount += annotations.size();

                        for (Annotation a : annotations) {

                            // retrieve resources from index
                            List<Instance> matches = getMatches(a.getWord(), indexSearch);

                            System.out.println(matches.size());

                            // if the link in annoation is redirect page
                            // replace it with the original one
                            // decoder the URI with UTF-8 encoding
                            String link = a.getLink();

                            link = URLDecoder.decode(link, "UTF-8");

                            a.setLink(link);

                            // if the retrieved list contains the link
                            // the index contains the annotation
                            boolean contains = false;

                            if (compareAll) {
                                for (Instance i1 : matches) {
                                    if (i1.getUri().equals(link)) {
                                        contains = true;
                                        break;
                                    }
                                }
                            } else {
                                if (!matches.isEmpty()) {
                                    if (matches.get(0).getUri().equals(link)) {
                                        contains = true;
                                    }
                                }
                            }

                            if (contains) {
                                Annotation newOne = a.clone();

                                d.addAnnotation(newOne.clone());
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
                        }
                    }

                    Map<String, Double> result = Evaluator.evaluateAll(docs);

                    for (String s : result.keySet()) {
                        if (s.equals("Micro-average Recall")) {
                            values.add(result.get(s));
                        }
                    }

                }

                System.out.println(dataset + "_" + approachPath);

                results.put(dataset + "_" + approachPath, values);
            }

        }

        String overallResult = "TopK;";

        //get dataset*approach names
        Object[] arrayOfDatasetsAppr = results.keySet().toArray();

        for (int l = 0; l < arrayOfDatasetsAppr.length; l++) {
            String k = arrayOfDatasetsAppr[l].toString();

            overallResult += k;

            if (l < arrayOfDatasetsAppr.length - 1) {
                overallResult += ";";
            }
        }

        overallResult += "\n";

        int counter = 0;
        for (int t = 1; t <= 100; t = t + 1) {

            if (t >= 11) {
                t += 9;
            }
            overallResult += t + ";";

            Object[] keys = results.keySet().toArray();

            for (int l = 0; l < keys.length; l++) {

                String k = keys[l].toString();
                List<Double> values = results.get(k);

                overallResult += values.get(counter);

                if (l < keys.length - 1) {
                    overallResult += ";";
                }
            }

            counter++;
            overallResult += "\n";
        }

        writeListToFile("Retrieval.txt", overallResult);
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

    private static List<Instance> getMatches(String word, CandidateRetrieverOnMemory indexSearch) {

        Set<String> queryTerms = new LinkedHashSet<>();
        queryTerms.add(word);

        List<Instance> temp = new ArrayList<>();
        // retrieve matches
        for (String q : queryTerms) {
            long start = System.nanoTime();
            temp.addAll(indexSearch.getAllResources(q, topK));

            long end = System.nanoTime();

            time += (end - start);
        }

        return temp;

    }
}
