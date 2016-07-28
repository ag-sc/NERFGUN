/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.DBpediaEndpoint;
import de.citec.sc.helper.NGramExtractor;
import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import java.util.Properties;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class TestCategory {

    private static NGramExtractor ngramExtractor = new NGramExtractor();

    public static void main(String[] args) {

        Set<String> classes = DBpediaEndpoint.getClasses("Barack_Obama");
        System.out.println(classes);
        Set<String> reduced = reduceClasses(classes, false);

        System.out.println(reduced);

//        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
//            System.out.println("---");
//            System.out.println("mentions");
//            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
//                System.out.println("\t" + m);
//            }
//        }
//        String e1 = "Wayne_Rooney";
//        String e2 = "Cristiano_Ronaldo";
//        String e3 = "Mexico";
//        
//        System.out.println(e1+" "+e2+" "+ DBpediaEndpoint.normalizedCategorySimilarity(e1, e2));
//        System.out.println(e1+" "+e3+" "+ DBpediaEndpoint.normalizedCategorySimilarity(e1, e3));
//        System.out.println(e3+" "+e2+" "+ DBpediaEndpoint.normalizedCategorySimilarity(e3, e2));
    }

    private static Set<String> reduceClasses(Set<String> classes, boolean superClass) {
        Set<String> reducedClasses = new HashSet<>();

        for (String c : classes) {
            //if old class c is superClass of newClass then add the new one

            if (reducedClasses.isEmpty()) {
                reducedClasses.add(c);
            } else {
                Set<String> temp = new HashSet<>();

                for (String added : reducedClasses) {
                    if(DBpediaEndpoint.isSubClass(added, c)){
                        
                        //if superclass are preferred
                        if(superClass){
                            temp.add(added);
                        }
                        else{
                            temp.add(c);
                        }
                        
                    }
                    else if(DBpediaEndpoint.isSubClass(c, added)){
                        
                        //if superclass are preferred
                        if(superClass){
                            temp.add(c);
                        }
                        else{
                            temp.add(added);
                        }
                        
                    }
                    //not related , add both
                    else{
                        temp.add(c);
                        temp.add(added);
                    }
                }
                
                reducedClasses.clear();
                reducedClasses.addAll(temp);
            }
        }

        return reducedClasses;
    }

    private static Set<String> extractNgrams(String text, int ngram_size, boolean removeStopWords) {
        Set<String> setOfNgrams = new HashSet<>();

        try {
            //extracts ngrams also removes stopwords if set to true
            //doesn't extract the unigrams
            ngramExtractor.extract(text.trim(), 2, ngram_size, removeStopWords);
            LinkedList<String> ngrams = ngramExtractor.getNGrams();

            //add ngrams
            for (String n : ngrams) {
                setOfNgrams.add(n.trim());
            }

            //add unigrams
            for (String n : text.split(" ")) {
                setOfNgrams.add(n.trim());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return setOfNgrams;
    }

    private static void getContextWords(String docText, int startIndex, int endIndex, int numberOfTokens) {
        String leftContextWords = "";
        String rightContextWords = "";

        String left = "", right = "";
        if (startIndex < docText.length()) {
            left = docText.substring(0, startIndex);
        }
        right = docText.substring(endIndex);

        String[] tokens = left.split(" ");

        int c = 0;
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (tokens[i].trim().length() > 2) {
                leftContextWords = tokens[i] + " " + leftContextWords;
                c++;
                if (c == numberOfTokens) {
                    break;
                }
            }

        }

        tokens = right.split(" ");

        c = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].trim().length() > 2) {
                rightContextWords += tokens[i] + " ";
                c++;
                if (c == numberOfTokens) {
                    break;
                }
            }
        }

        System.out.println("\t\tLeft: " + leftContextWords);
        System.out.println("\t\tRight: " + rightContextWords);
        System.out.println("\t\tNgrams: " + extractNgrams(leftContextWords.trim().toLowerCase() + " " + rightContextWords.trim().toLowerCase(), 3, true));
    }

    public static void coref() {

        CorpusLoader loader = new CorpusLoader(false);

        DefaultCorpus corpus = loader.loadCorpus(CorpusLoader.CorpusName.valueOf("CoNLLTestb"));
        List<Document> documents = corpus.getDocuments();

        String text = documents.get(documents.size() - 1).getDocumentContent().replace("\n", "");

        System.out.println(text);

        for (de.citec.sc.corpus.Annotation a1 : documents.get(documents.size() - 1).getGoldStandard()) {
            System.out.println(a1);
        }

        String[] sent = text.split("\\s[.]");
        List<String> sentencesFromText = new ArrayList<>();
        for (String s : sent) {
            if (s.equals("")) {
                System.out.println("Sentence: " + s);
            }
        }

        Annotation document = new Annotation(text);
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.annotate(document);
        System.out.println("---");
        System.out.println("coref chains");
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        List<String> sentencesFromCoreNLP = new ArrayList<>();

        for (CoreMap sentence : sentences) {

            sentencesFromCoreNLP.add(sentence.toString());
            System.out.println("Sentence : StanfordNLP: " + sentence);
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                String t = token.originalText();

            }

        }

        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {

            System.out.println("\t" + cc);

            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();

            System.out.println("\n\tHead: " + cc.getRepresentativeMention());
            for (CorefChain.CorefMention m : mentions) {
                System.out.println("\t\tDep: " + m + " EndIndex: " + m.endIndex + " StartIndex: " + m.startIndex);
            }

        }
    }

}
