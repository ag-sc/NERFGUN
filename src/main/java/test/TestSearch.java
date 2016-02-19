/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.Search;

/**
 *
 * @author sherzod
 */
public class TestSearch {

	public static void main(String[] args) throws UnsupportedEncodingException {

		Search indexSearch = new Search(false, "dbpediaIndexAll");

		List<String> terms = new ArrayList<>();
//		terms.add("U.S.");
		terms.add("Tartous");
//		terms.add("Metal");

		for (String term : terms) {
			List<String> anchor = indexSearch.getResourcesFromAnchors(term, 100);
			List<String> dbpedia = indexSearch.getResourcesFromDBpedia(term, 100);
			List<String> all = indexSearch.getAllResources(term, 100);
			System.out.println("---------------------------");
			System.out.println(String.format("%s: #%s:\n%s\n", "anchor", anchor.size(), anchor));
			System.out.println(String.format("%s: #%s:\n%s\n", "dbpedia", dbpedia.size(), dbpedia));
			System.out.println(String.format("%s: #%s:\n%s\n", "all", all.size(), all));
		}
		System.out.println();
		System.out.println();
		CorpusLoader loader = new CorpusLoader();
		DefaultCorpus c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLL);

		String path = "dbpediaFiles/pageranks.ttl";

		// read file into stream, try-with-resources
		ConcurrentHashMap<String, Double> map = new ConcurrentHashMap<>(19500000);

		String patternString = "<http://dbpedia.org/resource/(.*?)>.*\"(.*?)\"";
		Pattern pattern1 = Pattern.compile(patternString);

		Set<String> uris = new HashSet<>();
		for (Document d : c.getDocuments()) {
			for (Annotation a : d.getGoldResult()) {
				String uri = a.getLink().replace("http://en.wikipedia.org/wiki/", "");
				uris.add(uri);
			}
		}

		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			stream.parallel().forEach(item -> {

				String line = item.toString();

				Matcher m = pattern1.matcher(line);
				while (m.find()) {
					String uri = m.group(1);

					String r = m.group(2);
					Double v = Double.parseDouble(r);

					if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {
						try {
							// counter.incrementAndGet();
							uri = URLDecoder.decode(uri, "UTF-8");
						} catch (UnsupportedEncodingException ex) {
							Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
						}
						map.put(uri, v);

					}

				}

			});
			System.out.println(map.keySet().size());
			for (String uri : uris) {
				if (!map.keySet().contains(uri)) {
					System.out.println(uri);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		TreeMap<Integer, Double> ranges = new TreeMap<>();

		TreeSet<Double> values = new TreeSet<>();

		System.out.println(uris.size());
		for (String uri : uris) {
			if (map.containsKey(uri)) {
				Double v1 = map.get(uri);
				values.add(v1);
				// int v = (int)
				// ranges.put(v1, ranges.getOrDefault(v, 0) + 1);
			}
		}
		System.out.println(values.size());

		List<Double> valuesAsList = new ArrayList<>();
		valuesAsList.addAll(values);

		for (int i = 0; i < 10; i = i + 1) {
			double s1 = (((double) values.size()) / 10);
			int k = (int) (i * s1);

			System.out.println(valuesAsList.get(k));
		}

		////
		// int sum = 0;
		// for(Integer k : ranges.keySet()){
		// sum +=ranges.get(k);
		// System.out.println(k + "\t" + ranges.get(k) + " \t" + sum);
		// }
		//
		// ranges.entrySet().forEach(s -> System.out.println(s.getKey() + "\t" +
		//// s.getValue()));
	}
}
