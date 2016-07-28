package de.citec.sc.similarity.tfidf;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides methods to calcualte the term frequency and the document
 * frequency.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class TFIDF {

	/**
	 * Calculates the TF-IDF value for a given document.
	 * 
	 * @param document
	 *            the pre-tokenized document.
	 * @param DFs
	 *            the documenet frequency values for each term in the entire
	 *            corpus. (NOT THE IDF VALUES).
	 * @return the TFIDF value for the whole document.
	 * @throws IOException
	 */
	public static Map<String, Double> getTFWikiIDF(final List<String> document, Map<String, Double> DFs,
			Double numberOfDocumentsInCorpus) throws IOException {
		Map<String, Integer> TFs = getTFs(document);
		Map<String, Double> TFIDFs = new HashMap<String, Double>();

		for (String term : TFs.keySet()) {

			final double df = DFs.containsKey(term) ? numberOfDocumentsInCorpus / DFs.get(term) : 1;

			final double tfidf = ((double) TFs.get(term)) * Math.log(df);

			TFIDFs.put(term, tfidf);
		}

		return TFIDFs;
	}

	/**
	 * Calculates the term frequency for each term in the given document.
	 * 
	 * @param document
	 *            the pre-tokenized document.
	 * @param normalize
	 *            whether the term-frequency should be normalized or not.
	 * @return the term frequency for each term in the given document.
	 */
	public static final Map<String, Integer> getTFs(final List<String> document) {
		Map<String, Integer> tfs = new ConcurrentHashMap<>(document.size());

		document.stream().forEach(word -> {
			tfs.put(word, tfs.getOrDefault(word, 0) + 1);
		});

		// if (normalize) {
		// AtomicInteger sum = new AtomicInteger(0);
		// tfs.values().stream().forEach(d -> sum.addAndGet(d.intValue()));
		// tfs.entrySet().stream().forEach(e -> e.setValue(e.getValue() /
		// sum.get()));
		// }
		return tfs;
	}

	public static <A, B> Map<B, Double> getIDFs(final Set<B> dict, final Map<A, Set<B>> documents) {
		final double N = documents.size();
		Set<B> synchronizedDict = Collections.synchronizedSet(new HashSet<B>());
		Map<B, Double> termCounts = new ConcurrentHashMap<B, Double>();
		Map<B, Double> idfs = new ConcurrentHashMap<B, Double>();

		if (dict == null) {
			documents.entrySet().parallelStream().forEach(e -> e.getValue().stream().forEach(w -> {
				synchronized (dict) {
					synchronizedDict.add(w);
				}
			}));
		} else {
			synchronizedDict.addAll(dict);
		}

		dict.parallelStream().forEach(word -> {
			documents.values().stream().forEach(document -> {

				synchronized (termCounts) {

					termCounts.put(word, termCounts.getOrDefault(word, 0d) + (document.contains(word) ? 1d : 0d));
				}
			});
		});
		termCounts.entrySet().parallelStream().forEach(termCount -> {

			synchronized (idfs) {
				idfs.put(termCount.getKey(),
						(termCount.getValue().intValue()) == 0 ? 0 : Math.log(N / termCount.getValue().doubleValue()));
			}
		});
		return idfs;
	}
}
