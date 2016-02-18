/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.createIndex;

//github.com/ag-sc/NED.git
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.query.Search;

/**
 *
 * @author sherzod
 */
public class RetrievalPerformance {

	static Search indexSearch;

	static long time;
	static String index;
	private static int topK;

	public static void main(String[] args) throws UnsupportedEncodingException {
		run();
	}

	public static void run() throws UnsupportedEncodingException {
		List<Integer> topKs = new ArrayList<>();
		// topKs.add(10);
		topKs.add(100);
		// topKs.add(1000);
		// topKs.add(2000);

		List<String> datasets = new ArrayList<>();
		// datasets.add("tweets");
		datasets.add("news");
		// datasets.add("small");

		List<String> indexType = new ArrayList<>();
		// indexType.add("dbpedia");
		// indexType.add("anchors");
		indexType.add("all");

		List<String> dbindexPaths = new ArrayList<>();
		// dbindexPaths.add("dbpediaIndexOnlyLabels");
		// dbindexPaths.add("dbpediaIndexOnlyOntology");
		dbindexPaths.add("dbpediaIndexAll");

		List<Boolean> useMemory = new ArrayList<>();
		useMemory.add(Boolean.FALSE);
		// useMemory.add(Boolean.TRUE);

		CorpusLoader loader = new CorpusLoader(false);

		String overallResult = "";

		for (Boolean m : useMemory) {
			for (String indexT : indexType) {
				for (Integer t : topKs) {
					for (String dataset : datasets) {
						for (String dbpediaIndexPath : dbindexPaths) {

							topK = t;
							time = 0;
							index = indexT;

							long start = System.currentTimeMillis();
							indexSearch = new Search(m, dbpediaIndexPath);
							long end = System.currentTimeMillis();

							System.out.println((end - start) + " ms loading the index ");

							DefaultCorpus c = new DefaultCorpus();

							// set the dataset
							if (dataset.equals("tweets")) {
								c = loader.loadCorpus(CorpusLoader.CorpusName.MicroTagging);
							}
							if (dataset.equals("news")) {
								c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLL);
							}
							if (dataset.equals("small")) {
								c = loader.loadCorpus(CorpusLoader.CorpusName.SmallCorpus);
							}

							HashMap<String, Set<String>> notFound = new HashMap<String, Set<String>>();

							System.out.println(c.getDocuments().size());

							List<Document> docs = c.getDocuments();

							int annotationsCount = 0;

							for (Document d : docs) {

								List<Annotation> annotations = d.getGoldStandard();

								annotationsCount += annotations.size();

								for (Annotation a : annotations) {

									// retrieve resources from index
									Set<String> matches = getMatches(a.getWord());

									// if the link in annoation is redirect page
									// replace it with the original one
									// decoder the URI with UTF-8 encoding
									String link = a.getLink();

									link = URLDecoder.decode(link, "UTF-8");

									link = link.replace("http://en.wikipedia.org/wiki/",
											"http://dbpedia.org/resource/");

									a.setLink(link);

									// if the retrieved list contains the link
									// the index contains the annotation
									if (!matches.isEmpty()) {
										if (link.equals(matches.toArray()[0])) {
											Annotation newOne = new Annotation(a.getWord(), a.getLink(),
													a.getStartIndex(), a.getEndIndex(), a.getID());

											d.addAnnotation(newOne.clone());
										}
									}

									// if (matches.contains(link)) {
									// Annotation newOne = new
									// Annotation(a.getWord(), a.getLink(),
									// a.getStartIndex(), a.getEndIndex(),
									// a.getID());
									//
									// d.addAnnotation(newOne.clone());
									// }
									else {

										if (notFound.containsKey(a.getWord())) {
											Set<String> list = notFound.get(a.getWord());

											list.add(link);

											notFound.put(a.getWord(), list);
										} else {
											Set<String> list = new HashSet<>();

											list.add(link);

											notFound.put(a.getWord(), list);
										}

									}
								}

								System.out.println(docs.indexOf(d) + "  " + m + "  " + topK);
							}

							Evaluator eva = new Evaluator();

							Map<String, Double> result = eva.evaluateAll(docs);

							String s1 = "";
							for (String s : result.keySet()) {
								s1 += s + " " + result.get(s) + "\n";
								System.out.println(s + " " + result.get(s));
							}

							time = time / (long) annotationsCount;
							s1 += "\n\nRuntime per entity: " + time + " ms.";

							String n = "";
							for (String n1 : notFound.keySet()) {
								n += n1;
								for (String l : notFound.get(n1)) {
									n += "\t" + l;
								}
								n += "\n";
							}

							DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
							Date date = new Date();

							String stamp = dateFormat.format(date).replace(" ", "_");

							writeListToFile("retrieval/notFound_memory_" + m + "_top_" + topK + "_" + dataset
									+ "_index_" + index + "_property_" + dbpediaIndexPath + ".txt", n);
							writeListToFile("retrieval/results_memory_" + m + "_top_" + topK + "_" + dataset + "_index_"
									+ index + "_property_" + dbpediaIndexPath + ".txt", s1);

							overallResult += "memory_" + m + "_top_" + topK + "_" + dataset + "_index_" + index
									+ "_property_" + dbpediaIndexPath + "" + "\n\n" + s1 + "\n\n\n";
						}
					}
				}
			}
		}

		writeListToFile("overallResultFirstElement.txt", overallResult);
	}

	public static void writeListToFile(String fileName, String content) {
		try {
			File file = new File(fileName);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Set<String> getMatches(String word) {

		Set<String> queryTerms = new LinkedHashSet<>();
		queryTerms.add(word);
		queryTerms.add(word + "~");

		Set<String> temp = new LinkedHashSet<>();
		boolean lemmatize = true;
		boolean useWordNet = false;
		// retrieve matches
		for (String q : queryTerms) {
			long start = System.currentTimeMillis();
			if (index.contains("dbpedia")) {
				temp.addAll(indexSearch.getResourcesFromDBpedia(q, topK));
			}
			if (index.equals("anchors")) {
				temp.addAll(indexSearch.getResourcesFromAnchors(q, topK));
			}
			if (index.equals("all")) {
				temp.addAll(indexSearch.getAllResources(q, topK));
			}

			long end = System.currentTimeMillis();

			time += (end - start);
		}

		Set<String> result = new LinkedHashSet<>();

		return temp;

	}
}
