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
import de.citec.sc.query.CandidateRetrieverOnLucene;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.query.Instance;

/**
 *
 * @author sherzod
 */
public class RetrievalPerformance {

    static CandidateRetriever indexSearch;

    static long time;
    static String index;
    private static int topK;

    public static void main(String[] args) throws UnsupportedEncodingException {
        run();
    }

    public static void run() throws UnsupportedEncodingException {
        List<Integer> topKs = new ArrayList<>();
        // topKs.add(10);
        topKs.add(100);
//        topKs.add(200);
//        topKs.add(500);
        // topKs.add(1000);
        // topKs.add(2000);

        List<String> datasets = new ArrayList<>();
//        datasets.add("tweets");
        datasets.add("news");
        // datasets.add("small");

        boolean compareAll = false;

        List<String> indexType = new ArrayList<>();
        indexType.add("all");

        List<String> dbindexPaths = new ArrayList<>();
        // dbindexPaths.add("dbpediaIndexOnlyLabels");
        // dbindexPaths.add("dbpediaIndexOnlyOntology");
        dbindexPaths.add("dbpediaIndexAll");

        List<Boolean> useMemory = new ArrayList<>();
//        useMemory.add(Boolean.FALSE);
        useMemory.add(Boolean.TRUE);

        CorpusLoader loader = new CorpusLoader(false);

        String overallResult = "";

        for (Boolean m : useMemory) {
//            indexSearch = new CandidateRetrieverOnLucene(m, "mergedIndex");
            long start = System.currentTimeMillis();
            indexSearch = new CandidateRetrieverOnMemory();
            long end = System.currentTimeMillis();
            

            for (String indexT : indexType) {
                for (Integer t : topKs) {
                    for (String dataset : datasets) {
                        for (String dbpediaIndexPath : dbindexPaths) {

                            topK = t;
                            time = 0;
                            index = indexT;

                            DefaultCorpus c = new DefaultCorpus();

                            // set the dataset
                            if (dataset.equals("tweets")) {
                                c = loader.loadCorpus(CorpusLoader.CorpusName.MicroTagging);
                            }
                            if (dataset.equals("news")) {
                                c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLLTesta);
                            }

                            HashMap<String, Set<String>> notFound = new HashMap<String, Set<String>>();

                            System.out.println(c.getDocuments().size());

                            List<Document> docs = c.getDocuments();

                            int annotationsCount = 0;

                            for (Document d : docs) {

                                List<Annotation> annotations = d.getGoldStandard();

                                annotationsCount += annotations.size();

                                for (Annotation a : annotations) {

                                    // retrieve resources from index
                                    List<Instance> matches = getMatches(a.getWord());

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

                                System.out.println(docs.indexOf(d) + "  " + m + "  " + topK);
                            }

                            Map<String, Double> result = Evaluator.evaluateAll(docs);

                            String s1 = "";
                            for (String s : result.keySet()) {
                                s1 += s + " " + result.get(s) + "\n";
                                System.out.println(s + " " + result.get(s));
                            }

                            time = time / (long) annotationsCount;
                            s1 += "\n\nRuntime per entity: " + time + " nanoseconds.";

                            System.out.println(s1);

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

                            writeListToFile("retrieval/notFound_memory_" + m + "_top_" + topK + "_" + dataset
                                    + "_index_" + index + "_property_" + dbpediaIndexPath + ".txt", n);
                            writeListToFile("retrieval/results_memory_" + m + "_top_" + topK + "_" + dataset + "_index_"
                                    + index + "_property_" + dbpediaIndexPath + ".txt", s1);

                            overallResult += "memory_" + m + "_top_" + topK + "_" + dataset + "_index_" + index
                                    + "_property_" + dbpediaIndexPath + "" + "\n\n" + s1 + "\n\n\n";
                        }
                    }
                }
            }
            System.out.println((end - start) + " ms loading the index ");
            overallResult += (end - start) + " ms loading the index ";
        }

        writeListToFile("overallResult_compareAll_" + compareAll + ".txt", overallResult);
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

    private static List<Instance> getMatches(String word) {

        Set<String> queryTerms = new LinkedHashSet<>();
        queryTerms.add(word);

        List<Instance> temp = new ArrayList<>();
        // retrieve matches
        for (String q : queryTerms) {
            long start = System.nanoTime();
            if (index.equals("all")) {
                temp.addAll(indexSearch.getAllResources(q, topK));
            }

            long end = System.nanoTime();

            time += (end - start);
        }

        return temp;

    }
}
