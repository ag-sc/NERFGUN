/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.gerbil.GerbilUtil;
import de.citec.sc.gerbil.GsonDocument;
import de.citec.sc.helper.DocumentUtils;
import de.citec.sc.query.Instance;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;

/**
 *
 * @author sherzod
 */
public class Test {

    public static void main(String[] args) {

        Set<String> f = DocumentUtils.readFile(new File("gerbil_results.txt"));

        HashMap<String, HashMap<String, String>> values = new LinkedHashMap<>();

        for (String s : f) {
            if (!s.startsWith("Systems")) {
                String[] data = s.split("\t");

                if (data.length == 5) {
                    String system = data[0];
                    String dataset = data[1];
                    String microF1 = data[2];
                    String macroF1 = data[3];
                    String runtime = data[4];

                    if (values.containsKey(system)) {
                        HashMap<String, String> old = values.get(system);

                        old.put(dataset, microF1 + "\t" + macroF1 + "\t" + runtime);

                        values.put(system, old);
                    } else {
                        HashMap<String, String> old = new LinkedHashMap<>();

                        old.put(dataset, microF1 + "\t" + macroF1 + "\t" + runtime);

                        values.put(system, old);
                    }

                } else {

                    String system = data[0];
                    String dataset = data[1];

                    if (values.containsKey(system)) {
                        HashMap<String, String> old = values.get(system);

                        old.put(dataset, "Error\tError\tError");

                        values.put(system, old);
                    } else {
                        HashMap<String, String> old = new LinkedHashMap<>();

                        old.put(dataset, "Error\tError\tError");

                        values.put(system, old);
                    }

                }
            }
        }

        String header = "Systems";

        String content = "";
        List<String> lines = new ArrayList<>();

        for (String system : values.keySet()) {
            String rMicro = system + "\tMicro F1";
            String rMacro = system + "\tMacro F1";
            String runtime = system + "\tRuntime (ms)";

            for (String k : values.get(system).keySet()) {

                String v = values.get(system).get(k);

                String[] data = v.split("\t");
                rMicro += "\t" + data[0];
                rMacro += "\t" + data[1];
                runtime += "\t" + data[2];

                if (!header.contains(k)) {
                    header += "\t" + k;
                }
            }

            content += rMicro + "\n";
            content += rMacro + "\n";
            content += runtime + "\n";
        }

        content = header + "\n" + content.trim();

        DocumentUtils.writeListToFile("gerbil_cleaned.txt", content);

        System.out.println(content);

//        CorpusLoader l = new CorpusLoader(false);
//        DefaultCorpus c = l.loadCorpus(CorpusLoader.CorpusName.CoNLLFull);
//        
//        System.out.println(c.getDocuments().size());
//        int count =0;
//        for(Document d : c.getDocuments()){
//            if(!d.getGoldStandard().isEmpty())
//            {
//                count += d.getGoldStandard().size();
//            }
//        }
//        System.out.println(count);
//        testBIREAPI();
//        testGerbilAPI();
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
        String url = "http://purpur-v11:8181/NED";
        Document d1 = new Document("phone", "testDocument");
        Annotation a1 = new Annotation("phone", "", 0, 5);
        Annotation a2 = new Annotation("qdwtz", "", 5, 10);
        List<Annotation> goldAnnotations = new ArrayList<>();
        goldAnnotations.add(a1);
//        goldAnnotations.add(a2);
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
        String url = "http://purpur-v11:8181/bire";
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
