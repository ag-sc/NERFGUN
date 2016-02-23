package de.citec.sc.wikipedia.preprocess;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.citec.sc.helper.Stopwords;
import de.citec.sc.similarity.tfidf.IDFProvider;

public class WikipediaTFIDFVector {

	public static String dfFile = "en_wiki_large_abstracts.docfrequency";

	public static void main(String[] args) throws IOException {

		Map<String, Double> documentFrequency = IDFProvider.getIDF(dfFile);

		final double docNumber = countLines("gen/en_wiki_large_abstracts.termfrequency");

		PrintStream psOut = new PrintStream("gen/en_wiki_large_abstracts.tfidf");
		List<String> lines2Write = new ArrayList<String>();
		int countLines = 0;

		String line;

		BufferedReader termFreqStream = new BufferedReader(
				new FileReader(new File("gen/en_wiki_large_abstracts.termfrequency")));
		while ((line = termFreqStream.readLine()) != null) {
			countLines++;

			if (countLines % 1000 == 0) {
				System.out.println("Processed: " + countLines + " files.");
			}
			final String docID = line.split("\t", 2)[0] + "\t";
			StringBuffer line2Write = new StringBuffer(docID);

			final String[] terms = line.split("\t", 2)[1].split("\t");

			Map<String, Double> tfidfForDoc = new HashMap<String, Double>();

			/*
			 * rtfidf
			 */
			// double numOfTerms = 0;
			// for (String t : terms) {
			// String[] termData = t.split(" ");
			// numOfTerms += Integer.parseInt(termData[1]);
			// }

			/*
			 * atfidf
			 */
			double maxTermFreq = 0;
			for (String t : terms) {
				maxTermFreq = Math.max(Integer.parseInt(t.split(" ")[1]), maxTermFreq);
			}

			for (String t : terms) {
				String[] termData = t.split(" ");
				final String term = termData[0];

				if (Stopwords.ENGLISH_STOP_WORDS.contains(term))
					continue;

				final double normDocFreq = docNumber / documentFrequency.get(term);
				final double termFreq = Double.parseDouble(termData[1]);

				/*
				 * tfidf
				 */
				final double tfidf = termFreq * (normDocFreq == 0 ? 0 : Math.log(normDocFreq));
				tfidfForDoc.put(term, tfidf);

				/*
				 * idf
				 */
				// final double idf = (normDocFreq == 0 ? 0 :
				// Math.log(normDocFreq));
				// tfidfForDoc.put(term, idf);

				/*
				 * rtfidf
				 */
				// final double rtfidf = (termFreq / numOfTerms) * (normDocFreq
				// == 0 ? 0 : Math.log(normDocFreq));
				// tfidfForDoc.put(term, rtfidf);

				/*
				 * atfidf
				 */
				// final double atfidf = (termFreq / maxTermFreq) * (normDocFreq
				// == 0 ? 0 : Math.log(normDocFreq));
				// tfidfForDoc.put(term, atfidf);

			}

			writeCurrentVectorToFile(lines2Write, line2Write, tfidfForDoc, psOut);

		}

		lines2Write.forEach(psOut::println);

		termFreqStream.close();
		psOut.close();

	}

	private static void writeCurrentVectorToFile(List<String> lines2Write, StringBuffer line2Write,
			Map<String, Double> tfidfForDoc, PrintStream psOut) {
		tfidfForDoc.entrySet().stream().sorted(new Comparator<Entry<String, Double>>() {

			@Override
			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}

		}).forEach(termFreq -> line2Write.append(termFreq.getKey() + " " + termFreq.getValue() + "\t"));

		lines2Write.add(line2Write.toString().trim());
		if (lines2Write.size() == 100000) {
			lines2Write.forEach(psOut::println);
			lines2Write.clear();
		}
	}

	public static int countLines(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}
}
