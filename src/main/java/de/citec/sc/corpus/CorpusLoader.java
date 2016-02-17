/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;


import corpus.Corpus;
import corpus.LabeledInstance;
import de.citec.sc.corpus.Annotation;

import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import utility.VariableID;

/**
 *
 * @author sherzod
 */
public class CorpusLoader {

    private String datasetsPath = "";
    
    public CorpusLoader() {
        datasetsPath+="src/main/resources/";
    }

    public CorpusLoader(boolean isRun) {
        
    }
    
    
    public enum CorpusName {

        CoNLL, SmallCorpus, MicroTagging;
    }

    public DefaultCorpus loadCorpus(CorpusName corpusName) {
        DefaultCorpus c = new DefaultCorpus();

        List<Document> documents = new ArrayList<>();

        switch (corpusName) {
            case CoNLL:
                documents = getCONLLDocs();
                break;
            case MicroTagging:
                documents = getMicroTaggingDocs();
                break;
            case SmallCorpus:
                documents = getCONLLDocs().subList(0, 10);
                break;
        }

        c.addDocuments(documents);

        return c;
    }

    private List<String> readFileAsList(File file) {
        List<String> content = new ArrayList<>();
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                content.add(strLine);
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
        }

        return content;
    }

    private List<Document> getCONLLDocs() {
        String file = datasetsPath + "dataset/conll/dataset.tsv";

        List<String> list = readFileAsList(new File(file));

        List<Annotation> goldSet = new ArrayList<Annotation>();

        String s = "";

        List<Document> docs = new ArrayList<Document>();

        String docName = "";

        for (String l : list) {
            if (l.startsWith("-DOCSTART-")) {

                if (!s.equals("")) {
                    //write out

                    Document d1 = new Document(s, docName);
                    List<Annotation> annotations = new ArrayList<>();
                    for (Annotation ann : goldSet) {
                        annotations.add(ann.clone());
                    }
                    d1.setGoldStandard(annotations);

                    if(!d1.getGoldStandard().isEmpty()){
                        docs.add(d1);
                    }
                    

                    goldSet.clear();
                    s = "";
                }
                docName = l;
            } else {
                String[] content = l.split("\t");

                if (content.length == 6) {
                    //do B I I 
                    //European	B	European Commission	European_Commission
                    if (content[1].equals("B")) {

                        int startIndex = s.length();
                        int endIndex = s.length() + content[2].length();
                        Annotation a1 = new Annotation(content[2], content[4],startIndex, endIndex, new VariableID("A"+goldSet.size()));
                        goldSet.add(a1);
                        s += content[0] + " ";
                    } else {
                        s += content[0] + " ";
                    }
                }
                if (content.length == 4) {
                    //get string
                    s += content[0] + " ";
                }
                if (content.length == 1) {
                    if (l.equals("")) {
                        s += "\n";
                    } else {
                        s += content[0] + " ";
                    }
                }
            }
        }

        if (goldSet.size() > 0 && !s.equals("")) {

            //last doc
            Document d1 =  new Document(s, docName);
            
            List<Annotation> annotations = new ArrayList<>();
            for (Annotation ann : goldSet) {
                annotations.add(ann.clone());
            }
            d1.setGoldStandard(annotations);

            docs.add(d1);

            goldSet.clear();
            s = "";
        }

        return docs;
    }

    private List<Document> getMicroTaggingDocs() {
        List<Document> docs = new ArrayList<>();

        List<String> annotations = readFileAsList(new File(datasetsPath+ "dataset/microtag/dataset.in"));
        List<String> tweetDocs = readFileAsList(new File(datasetsPath + "dataset/microtag/dataset_tweets.out"));

        HashMap<String, Document> tweetsHashMap = new HashMap();

        for (String s : tweetDocs) {
            String docId = s.split("\t")[0];
            String content = s.replace(docId, "");
            content = content.trim();

            Document d1 = new Document(content, docId);

            tweetsHashMap.put(docId, d1);
        }

        for (String s : annotations) {
            String docId = s.split("\t")[0];
            String ans = s.replace(docId, "");
            ans = ans.trim();

            if (!ans.equals("")) {

                String[] arrayOfAnnotations = ans.split("\t");
                List<Annotation> goldSet = new ArrayList<>();

                for (int i = 0; i < arrayOfAnnotations.length; i = i + 2) {
                    Annotation a1 = new Annotation(arrayOfAnnotations[i], arrayOfAnnotations[i + 1], 0, 0, new VariableID("S"+goldSet.size()));
                    goldSet.add(a1);
                }

                if (tweetsHashMap.containsKey(docId)) {
                    Document d = tweetsHashMap.get(docId);

                    d.setGoldStandard(goldSet);

                    docs.add(d);
                }
            }

        }

        
        return docs;
    }
}
