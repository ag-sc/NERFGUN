package de.citec.sc.helper;

import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.hcoref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class StanfordParser {

    protected StanfordCoreNLP pipeline;
    MaxentTagger tagger;

    public StanfordParser() {
        // Create StanfordCoreNLP object properties, with POS tagging
        // (required for lemmatization), and lemmatization
        Properties props;
        props = new Properties();
//        props.put("annotators", "tokenize, ssplit,pos, lemma");
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
//        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
//        props.put("dcoref.score", true);

        // StanfordCoreNLP loads a lot of models, so you probably
        // only want to do this once per execution
        this.pipeline = new StanfordCoreNLP(props);
        tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
    }

    public void coref() {
        Annotation document = new Annotation("SOCCER - ENGLISHMAN CHARLTON IS MADE AN HONORARY IRISHMAN . \n"
                + "DUBLIN 1996-12-07 \n"
                + "Jack Charlton 's relationship with the people of Ireland was cemented on Saturday when the Englishman was officially declared one of their own . \n"
                + "Charlton , 61 , and his wife , Peggy , became citizens of Ireland when they formally received Irish passports from deputy Prime Minister Dick Spring who sai\n"
                + "d the honour had been made in recognition of Charlton 's achievements as the national soccer manager . \n"
                + "\" The years I spent as manager of the Republic of Ireland were the best years of my life . \n"
                + "It all culminated in the fact that I now have lots of great , great friends in Ireland . \n"
                + "That is why this is so emotional a night for me , \" Charlton said . \n"
                + "\" It was the joy that we all had over the period , that I shared with people that I grew to love , that I treasure most , \" he added . \n"
                + "Charlton managed Ireland for 93 matches , during which time they lost only 17 times in almost 10 years until he resigned in December 1995 . \n"
                + "He guided Ireland to two successive World Cup finals tournaments and to the 1988 European championship finals in Germany , after the Irish beat a well-fanci\n"
                + "ed England team 1-0 in their group qualifier . \n"
                + "The lanky former Leeds United defender did not make his England debut until the age of 30 but eventually won 35 caps and was a key member of the winning tea\n"
                + "m with his younger brother , .");

        pipeline.annotate(document);
        System.out.println("---");
        System.out.println("coref chains");
        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            System.out.println("\t" + cc);
        }
//        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
//            System.out.println("---");
//            System.out.println("mentions");
//            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
//                System.out.println("\t" + m);
//            }
//        }
    }


    public List<String> lemmatizeDocument(String documentText) {
        List<String> lemmas = new LinkedList<>();

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);

        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmas.add(token.get(LemmaAnnotation.class));
            }
        }

        return lemmas;
    }

    /**
     * postags the document and returns all tokens matched with postag key is
     * token, values are postags
     */
//    public Map<String, List<String>> tagDocument(String documentText) {
//        Map<String, List<String>> postags = new LinkedHashMap<>();
//
//        // create an empty Annotation just with the given text
//        Annotation document = new Annotation(documentText);
//
//        // run all Annotators on this text
//        this.pipeline.annotate(document);
//
//        // Iterate over all of the sentences found
//        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//        for (CoreMap sentence : sentences) {
//            // Iterate over all tokens in a sentence
//            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
//                // Retrieve and add the lemma for each word into the
//
//                List<String> data = new ArrayList<>();
//                data.add(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
//                data.add(token.beginPosition() + "");
//                data.add(token.endPosition() + "");
//                postags.put(token.originalText(), data);
//
//            }
//        }
//
//        return postags;
//    }
    public Map<String, String> tagDocument(String documentText) {
        Map<String, String> postags = new LinkedHashMap<>();

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);

        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the

                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                postags.put(token.originalText(), pos);

            }
        }

        return postags;
    }

    public String postagDocument(String documentText) {
        String tagged = tagger.tagString(documentText);

        return tagged;
    }

    /**
     *
     * @param t
     * @return
     */
    public String lemmatize(String t) {
        String lemma = "";
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(t);

        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemma += " " + token.get(LemmaAnnotation.class);

            }
        }

        return lemma.trim();
    }

    public static void main(String[] args) {
        StanfordParser lemmatizer = new StanfordParser();

        String doc = "Barack Ob_ama was reelected president in November 2012 !!!! He serves as a president.";

        List<String> s = lemmatizer.lemmatizeDocument(doc);
        System.out.println(s);

        String tagged = lemmatizer.postagDocument(doc);
        System.out.println(tagged);
        String[] data = tagged.split(" ");

        for (int i = 0; i < data.length; i++) {
            System.out.println(data[i].substring(data[i].lastIndexOf("_") + 1));
        }

        int z = 1;

    }

}
