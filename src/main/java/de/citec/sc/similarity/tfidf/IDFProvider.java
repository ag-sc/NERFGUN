package de.citec.sc.similarity.tfidf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides the IDF vector for all wikipedia files.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class IDFProvider {

	/**
	 * The actual vector.
	 */
	private static Map<String, Double> idf = null;

	/**
	 * Provides the IDF-vector. If the vector is not in the memory the method
	 * loads the IDF-vector from the hard-drive to the memory.
	 * 
	 * @return the entire IDF vector.
	 * @throws IOException
	 */
	public static Map<String, Double> getIDF() throws IOException {

		if (idf == null) {

			/*
			 * The file that stores the IDF data.
			 */
			BufferedReader docFreqStream = new BufferedReader(
					new FileReader(new File("en_wiki_large_abstracts.docfrequency")));
			/*
			 * The current line of the file.
			 */
			String line;

			idf = new HashMap<String, Double>();

			while ((line = docFreqStream.readLine()) != null) {
				String[] d = line.split("\t");
				idf.put(d[0], Double.parseDouble(d[1]));
			}
			docFreqStream.close();

		}
		return idf;

	}

}
