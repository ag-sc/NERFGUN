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
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
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

        try {
            // create an empty Annotation just with the given text
            Annotation document = new Annotation(documentText.replace("\n", ". "));

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

        } catch (Exception e) {
            System.out.println("DOC ERROR : " + documentText);
            e.printStackTrace();
//             String[] dummyTokens =  documentText.split(" ");
//             for(int i=0; i<dummyTokens.length; i++){
//                 lemmas.add(dummyTokens[i].trim());
//             }
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

        String doc = "mother teresa devoted to world s poor \n"
                + "CALCUTTA 1996-08-22 \n"
                + "mother teresa known as the saint of the gutters won the nobel peace prize in 1979 for bringing hope and dignity to millions of poor unwanted people with her simple message the poor must know that we love t\n"
                + "hem \n"
                + "while the world heaps honours on her and even regards her as a living saint the nun of albanian descent maintains she is merely doing god s work \n"
                + " it gives me great joy and fulfilment to love and care for the poor and neglected she said \n"
                + "the poor do not need our sympathy and pity \n"
                + "they need our love and compassion \n"
                + "the diminutive roman catholic missionary was on respiratory support in intensive care in an indian nursing home on thursday after suffering heart failure \n"
                + "but an attending doctor said mother teresa who turns 86 next tuesday was conscious and in stable condition \n"
                + "the task mother teresa began alone in 1949 in the slums of densely populated calcutta and grew to touch the hearts of people around the world \n"
                + "when in 1979 she was told she had won the nobel peace prize she said characteristically I am unworthy \n"
                + "the world disagreed showering more than 80 national and international honours on her including the bharat ratna or jewel of india the country s highest civilian award \n"
                + "her health began to deteriorate in 1989 when she was fitted with a heart pacemaker \n"
                + "A year later the vatican announced she was stepping down as superior of her missionaries of charity order \n"
                + "more than 100 delegates flew in from around the world to elect a successor \n"
                + "they could not agree so asked her to stay on \n"
                + "she agreed \n"
                + "in 1991 mother teresa was treated at a california hospital for heart disease and bacterial pneumonia \n"
                + "in 1993 she fell in rome and broke three ribs \n"
                + "in august the same year while in new delhi to receive yet another award she developed malaria complicated by her heart and lung problems \n"
                + "last april she fractured her left collar bone \n"
                + "but her increasing frailty arthritis and failing eyesight has not stopped her travels around the world to mingle with the poor and desperate \n"
                + "mother teresa was born agnes goinxha bejaxhiu to albanian parents in skopje in what was then serbia on august 27 1910 \n"
                + "she attended a government school and was already deeply religious by the time she was 12 \n"
                + "at the age of 18 she became a loretto nun hoping to work at the order s calcutta mission \n"
                + "she was sent to loretto abbey in dublin and from there to india to begin her novitiate and teach geography at a convent school in calcutta \n"
                + "she said her divine call to work among the poor came in september 1946 \n"
                + "the message was quite clear she told one interviewer \n"
                + "I was to leave the convent and help the poor while living among them \n"
                + "it was an order \n"
                + "I knew where I belonged \n"
                + "the vatican and the mother superior in dublin approved and after intensive training as a nurse with american missionaries she opened her first calcutta slum school in december 1949 \n"
                + "she took the name of after france s saint therese of the child jesus \n"
                + "in india she was simply called mother \n"
                + "mother teresa set up her first home for the dying in a hindu rest house in calcutta after she saw a penniless woman turned away by a city hospital \n"
                + "named nirmal hriday tender heart it was the first of a chain of 150 homes for dying destitute people admitting nearly 18,000 a year \n"
                + "her missionaries of charity a roman catholic religious order she founded in 1949 now runs about 300 homes for unwanted children and the destitute in india and abroad \n"
                + "in 1994 a british television documentary called the myth around mother teresa a mixture of hyperbole and credulity \n"
                + "catholics around the world rose to her defence";

        List<String> s = lemmatizer.lemmatizeDocument(doc.replace("\n", ". "));
        System.out.println(s);

//        String tagged = lemmatizer.postagDocument(doc);
//        System.out.println(tagged);
//        String[] data = tagged.split(" ");
//
//        for (int i = 0; i < data.length; i++) {
//            System.out.println(data[i].substring(data[i].lastIndexOf("_") + 1));
//        }

        int z = 1;

    }

}
