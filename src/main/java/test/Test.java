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
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;

/**
 *
 * @author sherzod
 */
public class Test {

    public static void main(String[] args) {

        String url = "http://purpur-v11:8080/ned/gerbil";
        Document d1 = new Document("phoneqdwtz", "testDocument");
        Annotation a1 = new Annotation("a1", "", 0, 5);
        Annotation a2 = new Annotation("a2", "", 5, 10);
        List<Annotation> goldAnnotations = new ArrayList<>();
        goldAnnotations.add(a1);
        goldAnnotations.add(a2);
        d1.setGoldStandard(goldAnnotations);

        GsonDocument gson = GerbilUtil.bire2gson(d1, true);
        org.aksw.gerbil.transfer.nif.Document d2 = GerbilUtil.gson2gerbil(gson);
        //String parameters = GerbilUtil.bire2json(d1, true);
        //String parameters2 = GerbilUtil.gerbil2json(d2);

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
