/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class CleanDataset {

    private HashMap<String, String> redirects = new LinkedHashMap<String, String>();

    public static void main(String[] args) {
        CleanDataset c = new CleanDataset();
        c.removeRedirectPages();
    }

    public void removeRedirectPages() {
        redirects = getRedirects(new File("dbpediaFiles/redirects_en.nt"));

        String file1 = "src/main/resources/dataset/conll/dataset.tsv";

        String fileOut = "src/main/resources/dataset/conll/cleaned_dataset.tsv";

        try {
            FileInputStream fstream = new FileInputStream(file1);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String line;
            List<String> processed = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] content = line.split("\t");

                if (content.length == 6) {
                    String redirectPage = content[4];
                    redirectPage = redirectPage.replace("http://en.wikipedia.org/wiki/", "");
                    
                    if (redirects.containsKey(redirectPage)) {
                        
                        String originalPage = redirects.get(redirectPage);
                        line = line.replace("http://en.wikipedia.org/wiki/"+redirectPage, "http://en.wikipedia.org/wiki/"+originalPage);

                        line = URLDecoder.decode(line, "UTF-8");
                        processed.add(line);

                    } else {
                        processed.add(line);
                    }
                } else {
                    processed.add(line);
                }
            }

            writeListToFile(fileOut, processed);

            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeListToFile(String fileName, List<String> content) {
        try {
            File file = new File(fileName);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("");

            for (String s : content) {
                bw.append(s + "\n");
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> getRedirects(File file) {
        HashMap<String, String> content = new HashMap<>();

        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = br.readLine()) != null) {

                if (!line.startsWith("#")) {
                    //System.out.println(line);
                    String[] a = line.split(" ");

                    String s = a[0];

                    String p = a[1];

                    String o = a[2];

                    s = s.replace("<", "");
                    s = s.replace(">", "");
                    p = p.replace("<", "");
                    p = p.replace(">", "");
                    o = o.replace("<", "");
                    o = o.replace(">", "");

                    s = s.replace("http://dbpedia.org/resource/", "");
                    o = o.replace("http://dbpedia.org/resource/", "");
                    
                    o = URLDecoder.decode(o, "UTF-8");
                    s = URLDecoder.decode(s, "UTF-8");

                    content.put(s, o);
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return content;
    }
}
