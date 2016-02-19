package de.citec.sc.similarity.tfidf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
		Map<String, Double> TFs = getTFs(document, false);
		Map<String, Double> TFIDFs = new HashMap<String, Double>();

		for (String term : TFs.keySet()) {

			final double df = DFs.containsKey(term) ? numberOfDocumentsInCorpus / DFs.get(term) : 1;

			final double tfidf = TFs.get(term) * Math.log(df);

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
	public static final Map<String, Double> getTFs(final List<String> document, boolean normalize) {
		Map<String, Double> tfs = new ConcurrentHashMap<String, Double>(document.size());

		document.stream().forEach(word -> {
			tfs.put(word, tfs.getOrDefault(word, 0d) + 1);
		});

		if (normalize) {
			AtomicInteger sum = new AtomicInteger(0);
			tfs.values().stream().forEach(d -> sum.addAndGet(d.intValue()));
			tfs.entrySet().stream().forEach(e -> e.setValue(e.getValue() / sum.get()));
		}
		return tfs;
	}

}
