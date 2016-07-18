/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;

/**
 *
 * @author sherzod
 */
public class TestCategory {

    public static void main(String[] args) {
        
                CorpusLoader loader = new CorpusLoader(false);

        DefaultCorpus corpus = loader.loadCorpus(CorpusLoader.CorpusName.valueOf("CoNLLTestb"));
        List<Document> documents = corpus.getDocuments();
        
        String text = documents.get(documents.size()-1).getDocumentContent().replace("\n", "");
        
        System.out.println(text);
        
        for(de.citec.sc.corpus.Annotation a1 : documents.get(documents.size()-1).getGoldStandard()){
            System.out.println(a1);
        }
        
        String[] sent = text.split("\\s[.]");
        List<String> sentencesFromText = new ArrayList<>();
        for(String s : sent){
            if(s.equals(""))
            System.out.println("Sentence: "+s);
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
            System.out.println("Sentence : StanfordNLP: "+sentence);
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                String t = token.originalText();
                
            }
            
        }
        
        
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            
            System.out.println("\t" + cc);
            
            
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();
            
            System.out.println("\n\tHead: "+ cc.getRepresentativeMention());
            for(CorefChain.CorefMention m : mentions){
                System.out.println("\t\tDep: "+m+" EndIndex: "+m.endIndex+" StartIndex: "+m.startIndex);
            }
            
            
            
        }
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

}
