package de.citec.sc.helper;

import java.util.ArrayList;
import java.util.List;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import edu.stanford.nlp.util.ArraySet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static javafx.scene.input.KeyCode.K;
import static javafx.scene.input.KeyCode.V;

public class DocumentUtils {

    public static List<Document> splitDocument(Document d, int capacity) {
        List<Document> splitted = new ArrayList<>();
        if (d.getGoldStandard().size() > capacity) {

            List<Annotation> resizedGoldStandard = new ArrayList<>();

            for (int j = 0; j < d.getGoldStandard().size(); j++) {
                if (resizedGoldStandard.size() == 50) {
                    Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
                    d1.setGoldStandard(resizedGoldStandard);
                    resizedGoldStandard = new ArrayList<>();

                    splitted.add(d1);

                    resizedGoldStandard.add(d.getGoldStandard().get(j));

                } else {
                    resizedGoldStandard.add(d.getGoldStandard().get(j));
                }
            }

            // add the remaining
            if (resizedGoldStandard.size() <= 20) {
                splitted.get(splitted.size() - 1).getGoldStandard().addAll(resizedGoldStandard);
            } else {
                // create a new document
                Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
                d1.setGoldStandard(resizedGoldStandard);
                resizedGoldStandard = new ArrayList<>();

                splitted.add(d1);

            }
        } else {
            Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
            d1.setGoldStandard(d.getGoldStandard());

            splitted.add(d1);
        }
        return splitted;
    }
    
    public static Set<String> readFile(File file) {
        Set<String> content = null;
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                if (content == null) {
                    content = new ArraySet<>();
                }
                content.add(strLine);
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return content;
    }

    public static void writeStringToFile(String fileName, String content, boolean append) {
        try {
            File file = new File(fileName);

            // if file doesnt exists, then create it
            
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file,append);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            pw.println(content);
            
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void writeCollectionToFile(String fileName, Collection<String> content, boolean append) {
        try {
            File file = new File(fileName);

            // if file doesnt exists, then create it
            
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file,append);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            for(String s : content){
                pw.println(s);
            }
            
            
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
  

    public static void writeMapToFile(String fileName, Map content, boolean append) {
        try {
            File file = new File(fileName);

            // if file doesnt exists, then create it
            
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file,append);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            for(Object s : content.keySet()){
                pw.println(s+"\t"+content.get(s));
            }
            
            
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
