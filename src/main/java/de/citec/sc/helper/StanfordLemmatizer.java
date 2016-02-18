package de.citec.sc.helper;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 *
 * @author sherzod
 */
public class StanfordLemmatizer {

	protected StanfordCoreNLP pipeline;

	public StanfordLemmatizer() {
		// Create StanfordCoreNLP object properties, with POS tagging
		// (required for lemmatization), and lemmatization
		Properties props;
		props = new Properties();
		props.put("annotators", "tokenize, ssplit,pos, lemma");

		// StanfordCoreNLP loads a lot of models, so you probably
		// only want to do this once per execution
		this.pipeline = new StanfordCoreNLP(props);
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
		StanfordLemmatizer lemmatizer = new StanfordLemmatizer();

		String doc = "Obama was reelected president in November 2012, defeating Republican nominee Mitt Romney, and was sworn in for a second term on January 20, 2013. During his second term, Obama has promoted domestic policies related to gun control in response to the Sandy Hook Elementary School shooting, and has called for greater inclusiveness for LGBT Americans, while his administration has filed briefs which urged the Supreme Court to strike down part of the federal Defense of Marriage Act and state level same-sex marriage bans as unconstitutional. In foreign policy, Obama ordered U.S. military intervention in Iraq in response to gains made by the Islamic State after the 2011 withdrawal from Iraq, continued the process of ending U.S. combat operations in Afghanistan, promoted discussions that led to the 2015 Paris Agreement on global climate change, brokered a nuclear deal with Iran, and normalized U.S. relations with Cuba.";

		List<String> s = lemmatizer.lemmatizeDocument(doc);
		System.out.println(s);
		int z = 1;
	}

}