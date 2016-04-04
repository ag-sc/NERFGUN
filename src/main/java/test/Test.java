/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.formats.bire.BireDataLine;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.weka.WekaRegression;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import learning.scorer.Scorer;

/**
 *
 * @author sherzod
 */
public class Test {

    public static void main(String[] args) {

        
        String address = "http://purpur-v11:8080/ned/gerbil";
        Document d1 = new Document("", "");
        Annotation a1 = new Annotation("obama", "Barack_Obama", 1, 6);
        List<Annotation> goldAnnotations = new ArrayList<>();
        goldAnnotations.add(a1);
        d1.setGoldStandard(goldAnnotations);
        
        
        

        try {
            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            Gson gson = new GsonBuilder().create();

            
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    

//        String path = "dbpediaFiles/pageranks.ttl";
//
//        // read file into stream, try-with-resources
//        ConcurrentHashMap<String, Double> map = new ConcurrentHashMap<>(19500000);
//
//        String patternString = "<http://dbpedia.org/resource/(.*?)>.*\"(.*?)\"";
//        Pattern pattern1 = Pattern.compile(patternString);
//
//        Set<String> uris = new HashSet<>();
//        for (Document d : c.getDocuments()) {
//            for (Annotation a : d.getGoldResult()) {
//                String uri = a.getLink().replace("http://en.wikipedia.org/wiki/", "");
//                uris.add(uri);
//            }
//        }
//
//        try (Stream<String> stream = Files.lines(Paths.get(path))) {
//            stream.parallel().forEach(item -> {
//
//                String line = item.toString();
//
//                Matcher m = pattern1.matcher(line);
//                while (m.find()) {
//                    String uri = m.group(1);
//
//                    String r = m.group(2);
//                    Double v = Double.parseDouble(r);
//
//                    if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
//                        try {
//                            // counter.incrementAndGet();
//                            uri = URLDecoder.decode(uri, "UTF-8");
//                        } catch (UnsupportedEncodingException ex) {
//                            Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                        map.put(uri, v);
//
//                    }
//
//                }
//
//            });
//            System.out.println(map.keySet().size());
//            for (String uri : uris) {
//                if (!map.keySet().contains(uri)) {
//                    System.out.println(uri);
//                }
//
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        TreeMap<Integer, Double> ranges = new TreeMap<>();
//
//        TreeSet<Double> values = new TreeSet<>();
//
//        System.out.println(uris.size());
//        for (String uri : uris) {
//            if (map.containsKey(uri)) {
//                Double v1 = map.get(uri);
//                values.add(v1);
//				// int v = (int)
//                // ranges.put(v1, ranges.getOrDefault(v, 0) + 1);
//            }
//        }
//        System.out.println(values.size());
//
//        List<Double> valuesAsList = new ArrayList<>();
//        valuesAsList.addAll(values);
//
//        for (int i = 0; i < 10; i = i + 1) {
//            double s1 = (((double) values.size()) / 10);
//            int k = (int) (i * s1);
//
//            System.out.println(valuesAsList.get(k));
//        }
    }

}
