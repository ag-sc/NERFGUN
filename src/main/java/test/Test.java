/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.gerbil.GerbilUtil;
import de.citec.sc.gerbil.GsonDocument;
import de.citec.sc.helper.DBpediaEndpoint;
import de.citec.sc.helper.DocumentUtils;
import de.citec.sc.helper.SortUtils;
import de.citec.sc.query.Instance;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;

/**
 *
 * @author sherzod
 */
public class Test {

    public static void main(String[] args) {
//
////        DBpediaEndpoint.init();
//        HashMap<String, Integer> freqCategory = new HashMap<>();
//
//        CorpusLoader loader = new CorpusLoader(false);
//
//        DefaultCorpus corpus = loader.loadCorpus(CorpusLoader.CorpusName.valueOf("CoNLLTraining"));
//        List<Document> documents = corpus.getDocuments();
//
//        Set<String> stopwords = DocumentUtils.readFile(new File("src/main/resources/stopwords"));
//
//        for (Document d : documents.subList(1, 2)) {
//            System.out.println(documents.indexOf(d));
//            
//            System.out.println(d.getDocumentContent()+"\n\n");
//
//            String[] text = d.getDocumentContent().split(" ");
//            HashMap<String, Integer> commontWordsFromCat = new LinkedHashMap<>();
//            HashMap<String, Integer> commontWordsFromText = new LinkedHashMap<>();
//            
//            for (String w : text) {
//                w = w.trim().toLowerCase();
//                
//                if (!stopwords.contains(w) && w.length() > 3) {
//                    commontWordsFromText.put(w, commontWordsFromText.getOrDefault(w, 1) + 1);
//                }
//            }
//
//            for (Annotation a : d.getGoldResult()) {
//                commontWordsFromText.put(a.getWord().toLowerCase(), commontWordsFromText.getOrDefault(a.getWord().toLowerCase(), 1) + 1);
//                
//                String link = a.getLink();
//
//                Set<String> categories = DBpediaEndpoint.getCategories(link);
//                Set<String> parentCategories = DBpediaEndpoint.getParentCategories(categories);
//                Set<String> parent_2_Categories = DBpediaEndpoint.getParentCategories(parentCategories);
//
//                //the child categories are also part of this set
//                for (String c : parent_2_Categories) {
//                    freqCategory.put(c, freqCategory.getOrDefault(c, 1) + 1);
//                }
//
//                for (String c : categories) {
//                    String[] words = c.split("_");
//                    for (String w : words) {
//                        w = w.trim().toLowerCase();
//
//                        if (!stopwords.contains(w) && w.length() > 3) {
//                            commontWordsFromCat.put(w, commontWordsFromCat.getOrDefault(w, 1) + 1);
//                        }
//                    }
//                }
//            }
//            
//            commontWordsFromCat = SortUtils.sortByValue(commontWordsFromCat);
//            commontWordsFromText = SortUtils.sortByValue(commontWordsFromText);
//            
//            int z =0;
//            for(String w : commontWordsFromCat.keySet()){
//                System.out.println(w+ " "+commontWordsFromCat.get(w));
//                z++;
//                if(z==10){
//                    break;
//                }
//            }
//            
//            System.out.println("=======================================");
//            z =0;
//            for(String w : commontWordsFromText.keySet()){
//                System.out.println(w+ " "+commontWordsFromText.get(w));
//                z++;
//                if(z==40){
//                    break;
//                }
//            }
//        }
//
//        freqCategory = SortUtils.sortByValue(freqCategory);
//
//        int sum = 0;
//        for (String s : freqCategory.keySet()) {
//
//        }
//        File f = new File("freqCategories.txt");
//        if (f.exists()) {
//            f.delete();
//        }
//
//        for (String s : freqCategory.keySet()) {
//            DocumentUtils.writeListToFile("freqCategories.txt", s.replace("http://dbpedia.org/resource/", "") + "   " + freqCategory.get(s), true);
////            System.out.println(s.replace("http://dbpedia.org/resource/", "")+"   "+freqCategory.get(s));
//        }

//        System.out.println(documents.size());
//        testBIREAPI();
//        testGerbilAPI();
        gerbilResults();
    }

    private static double sigmoid(double x) {
        return (1.0 / (1 + Math.pow(Math.E, -x)));
    }

    private static boolean isAbbreviation(String node, String uri) {
        String abbr = node.length() > uri.length() ? uri : node;
        String word = node.length() > uri.length() ? node : uri;

        abbr = abbr.replace(".", "");

        String[] tokens = word.split(" ");

        if (tokens.length != abbr.length()) {
            return false;
        }

        int count = 0;
        for (int i = 0; i < abbr.length(); i++) {
            String c = abbr.charAt(i) + "";
            if (tokens[i].startsWith(c)) {
                count++;
            }
        }

        if (count == abbr.length()) {
            return true;
        }

        return false;
    }

    public static void gerbilResults() {
        Set<String> f = DocumentUtils.readFile(new File("gerbil_results.txt"));

        HashMap<String, HashMap<String, String>> values = new LinkedHashMap<>();

        DecimalFormat f1Format = new DecimalFormat("#.##");
        f1Format.setRoundingMode(RoundingMode.CEILING);

        DecimalFormat runtimeFormat = new DecimalFormat("#");
        runtimeFormat.setRoundingMode(RoundingMode.CEILING);

        for (String s : f) {
            if (!s.startsWith("Systems")) {
                String[] data = s.split("\t");

                if (data.length == 5) {
                    String system = data[0];
                    String dataset = data[1];
                    String microF1 = data[2];
                    String macroF1 = data[3];
                    String runtime = data[4];
                    
                    if(dataset.contains("Derczynski") || dataset.contains("Microposts2013-Test") || dataset.contains("Microposts2013-Train")){
                        continue;
                    }

                    if (values.containsKey(system)) {
                        HashMap<String, String> old = values.get(system);

                        old.put(dataset, f1Format.format(Double.parseDouble(microF1)) + "\t" + f1Format.format(Double.parseDouble(macroF1)) + "\t" + runtimeFormat.format(Double.parseDouble(runtime)));

                        values.put(system, old);
                    } else {
                        HashMap<String, String> old = new LinkedHashMap<>();

                        old.put(dataset, f1Format.format(Double.parseDouble(microF1)) + "\t" + f1Format.format(Double.parseDouble(macroF1)) + "\t" + runtimeFormat.format(Double.parseDouble(runtime)));

                        values.put(system, old);
                    }

                } else {

                    String system = data[0];
                    String dataset = data[1];
                    if(dataset.contains("Derczynski") || dataset.contains("Microposts2013-Test") || dataset.contains("Microposts2013-Train")){
                        continue;
                    }
                    

                    if (values.containsKey(system)) {
                        HashMap<String, String> old = values.get(system);

                        old.put(dataset, "N/A\tN/A\tN/A");

                        values.put(system, old);
                    } else {
                        HashMap<String, String> old = new LinkedHashMap<>();

                        old.put(dataset, "N/A\tN/A\tN/A");

                        values.put(system, old);
                    }

                }
            }
        }

        String header = "Systems\tMeasures";

        String content = "";
        List<String> lines = new ArrayList<>();

        for (String system : values.keySet()) {
            String rMicro = system + "\tMicro F1";
            String rMacro = system + "\tMacro F1";
//            String runtime = system + "\tRuntime (ms)";

            for (String k : values.get(system).keySet()) {

                String v = values.get(system).get(k);

                String[] data = v.split("\t");
                rMicro += "\t" + data[0];
                rMacro += "\t" + data[1];
//                runtime += "\t" + data[2];

                if (!header.contains(k)) {
                    header += "\t" + k;
                }
            }

            content += rMicro + "\n";
            content += rMacro + "\n";
//            content += runtime + "\n";
        }

        content = header + "\n" + content.trim();

        DocumentUtils.writeListToFile("gerbil_cleaned.txt", content, false);

        System.out.println(content);
    }

    private static List<Instance> filterNumbers(List<Instance> candidates, String text) {
        List<Instance> filtered = new ArrayList<>();

        String regexYear = "^[12][0-9]{3}$";//Years from 1000 to 2999
        String regexNumber = "^\\d{1,3}$";//two digit numbers

        HashMap<String, String> numbers = new HashMap<>();
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        numbers.put("five", "5");
        numbers.put("six", "6");
        numbers.put("seven", "7");
        numbers.put("eight", "8");
        numbers.put("nine", "9");
        numbers.put("ten", "10");
        numbers.put("eleven", "11");
        numbers.put("twelve", "12");
        numbers.put("thirteen", "13");

        if (text.matches(regexYear)) {

            for (Instance i : candidates) {
                if (text.equalsIgnoreCase(i.getUri())) {
                    filtered.add(i);

                    return filtered;
                }
            }
        } else if (text.matches(regexNumber)) {

            for (Instance i : candidates) {

                if (i.getUri().equalsIgnoreCase(text + "_(number)")) {
                    filtered.add(i);
                    return filtered;
                }
            }
        } else if (numbers.containsKey(text.toLowerCase())) {

            for (Instance i : candidates) {

                if (i.getUri().equalsIgnoreCase(numbers.get(text) + "_(number)")) {
                    filtered.add(i);
                    return filtered;
                }
            }
        }

        return candidates;
    }

    public static void testGerbilAPI() {
        String url = "http://psink.techfak.uni-bielefeld.de/ned";
        Document d1 = new Document("Obamaesqdwtz", "testDocument");
        Annotation a1 = new Annotation("Obama", "", 0, 6);
        Annotation a2 = new Annotation("qdwtz", "", 5, 10);
        List<Annotation> goldAnnotations = new ArrayList<>();
        goldAnnotations.add(a1);
        goldAnnotations.add(a2);
        d1.setGoldStandard(goldAnnotations);

        GsonDocument gson = GerbilUtil.bire2gson(d1, true);
        org.aksw.gerbil.transfer.nif.Document d2 = GerbilUtil.gson2gerbil(gson);

        TurtleNIFDocumentCreator creator = new TurtleNIFDocumentCreator();
        String nifDoc = creator.getDocumentAsNIFString(d2);

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(nifDoc);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + nifDoc);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testBIREAPI() {
        String url = "http://purpur-v11:8080/bire";
        Document d1 = new Document("phoneqdwtz", "testDocument");
        Annotation a1 = new Annotation("St. John Fisher", "", 0, 5);
        Annotation a2 = new Annotation("2", "", 5, 10);
        List<Annotation> goldAnnotations = new ArrayList<>();
        goldAnnotations.add(a1);
        goldAnnotations.add(a2);
        d1.setGoldStandard(goldAnnotations);

        String parameters = GerbilUtil.bire2json(d1, true);

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(parameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + parameters);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
