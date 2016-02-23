package de.citec.sc.wikipedia.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WikipediaTermFrequency {

	public static void main(String[] args) throws IOException {

		BufferedReader br = new BufferedReader(
				new FileReader(new File("gen/en_wiki_large_abstracts_regexp_lemmat_token_ized.txt")));
		String line;

		int count = 0;

		PrintStream psOut = new PrintStream("gen/en_wiki_large_abstracts.termfrequency");

		List<String> lines2Write = new ArrayList<String>();

		while ((line = br.readLine()) != null) {

			Map<String, Integer> termFrequency = new HashMap<String, Integer>();
			count++;
			final String docID = line.split("\t", 2)[0];
			StringBuffer line2Write = new StringBuffer(docID + "\t");
			final String[] terms = line.split("\t", 2)[1].split(" ");
			for (int i = 0; i < terms.length; i++) {
				termFrequency.put(terms[i], termFrequency.getOrDefault(terms[i], 0) + 1);
			}
			if (count % 1000 == 0) {
				System.out.println("Processed: " + count + " files.");
			}
			termFrequency.entrySet().stream().sorted(new Comparator<Entry<String, Integer>>() {

				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return -o1.getValue().compareTo(o2.getValue());
				}

			}).forEach(termFreq -> line2Write.append(termFreq.getKey() + " " + termFreq.getValue() + "\t"));

			lines2Write.add(line2Write.toString().trim());
			if (lines2Write.size() == 100000) {
				lines2Write.forEach(psOut::println);
				lines2Write.clear();
			}

		}
		lines2Write.forEach(psOut::println);

		psOut.close();
		br.close();

	}
}
