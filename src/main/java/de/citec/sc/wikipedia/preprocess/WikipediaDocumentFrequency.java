package de.citec.sc.wikipedia.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WikipediaDocumentFrequency {

	public static void main(String[] args) throws IOException {

		BufferedReader br = new BufferedReader(
				new FileReader(new File("gen/en_wiki_large_abstracts_regexp_lemmat_token_ized.txt")));
		String line;

		Map<String, Integer> documentFrequency = new HashMap<String, Integer>();
		int count = 0;

		PrintStream psOut = new PrintStream("gen/en_wiki_large_abstracts.docfrequency");

		while ((line = br.readLine()) != null) {
			count++;
			final String[] termsAsArray = line.split("\t", 2)[1].split(" ");

			Set<String> terms = new HashSet<String>(Arrays.asList(termsAsArray));

			for (String term : terms) {
				documentFrequency.put(term, documentFrequency.getOrDefault(term, 0) + 1);

			}

			if (count % 1000 == 0) {
				System.out.println("Processed: " + count + " files.");
			}
		}

		documentFrequency.entrySet().stream().sorted(new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}

		}).forEach(docFrequency -> psOut.println(docFrequency.getKey() + "\t" + docFrequency.getValue()));

		psOut.close();
		br.close();

	}
}
