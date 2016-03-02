/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.SingleVariablePattern;
import learning.Vector;

/**
 *
 * @author sherzod
 */
public class PageRankTemplate extends templates.AbstractTemplate<Document, State, SingleVariablePattern<Annotation>> {

	private static org.apache.logging.log4j.Logger log = LogManager.getFormatterLogger();

	ConcurrentHashMap<String, Double> pageRankMap;

	public PageRankTemplate() {
		if ((pageRankMap == null) || (pageRankMap.isEmpty())) {
			loadPageRanks();
		}

	}

	@Override
	public Set<SingleVariablePattern<Annotation>> generateFactorPatterns(State state) {
		Set<SingleVariablePattern<Annotation>> factors = new HashSet<>();
		for (Annotation a : state.getEntities()) {
			factors.add(new SingleVariablePattern<>(this, a));
		}
		log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	public void computeFactor(Document instance, Factor<SingleVariablePattern<Annotation>> factor) {
		Annotation entity = factor.getFactorPattern().getVariable();
		log.debug("Compute %s factor for variable %s", PageRankTemplate.class.getSimpleName(), entity);
		Vector featureVector = factor.getFeatureVector();
		String uri = entity.getLink();
		Double score = pageRankMap.get(uri);
		if (score == null) {
			score = 0.0;
		}

		String pageRankPrefix = "PageRank";

		featureVector.set(pageRankPrefix, score);

		// featureVector.set("PageRank_HIGHER_THAN_100", rank > 100 ? 1.0 : 0);
	}

	// private void loadPageRanks() {
	//
	// if (pageRankMap == null) {
	//
	// pageRankMap = new ConcurrentHashMap<>(19500000);
	//// String path = "dbpediaFiles/pageranks.ttl";
	// String path = "pagerank.csv";
	// String patternString = "<http://dbpedia.org/resource/(.*?)>.*\"(.*?)\"";
	// Pattern pattern1 = Pattern.compile(patternString);
	//
	// System.out.print("Loading pagerank scores to memory ...");
	//
	// try (Stream<String> stream = Files.lines(Paths.get(path))) {
	// stream.parallel().forEach(item -> {
	//
	// String line = item.toString();
	//
	// Matcher m = pattern1.matcher(line);
	// while (m.find()) {
	// String uri = m.group(1);
	//
	// String r = m.group(2);
	// Double v = Double.parseDouble(r);
	//
	// if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
	// try {
	// //counter.incrementAndGet();
	// uri = URLDecoder.decode(uri, "UTF-8");
	// } catch (UnsupportedEncodingException ex) {
	// Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
	// }
	// pageRankMap.put(uri, v);
	//
	// }
	//
	// }
	//
	// });
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// System.out.println(" DONE");
	// }
	//
	// }
	private void loadPageRanks() {

		pageRankMap = new ConcurrentHashMap<>(19500000);
		// String path = "dbpediaFiles/pageranks.ttl";
		String path = "pagerank.csv";

		System.out.print("Loading pagerank scores to memory ...");

		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			stream.parallel().forEach(item -> {

				String line = item.toString();

				String[] data = line.split("\t");
				String uri = data[1];
				Double v = Double.parseDouble(data[2]);
				if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {

					uri = StringEscapeUtils.unescapeJava(uri);

					try {
						uri = URLDecoder.decode(uri, "UTF-8");
					} catch (Exception e) {
					}

					pageRankMap.put(uri, v);

				}

			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("  DONE");

		System.out.println(pageRankMap.get("Germany"));
		System.out.println(pageRankMap.get("History_of_Germany"));
		System.out.println(pageRankMap.get("German_language"));

		int z = 1;
	}

}
