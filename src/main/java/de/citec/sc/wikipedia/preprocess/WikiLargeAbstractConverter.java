package de.citec.sc.wikipedia.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import de.citec.sc.helper.StanfordLemmatizer;
import de.citec.sc.helper.Tokenizer;

public class WikiLargeAbstractConverter {

	final static String prefix = "<http://dbpedia.org/resource/";

	public static void main(String[] args) throws IOException {

		tmp();

		// constructFromRaw();

	}

	private static void tmp() throws IOException {
		BufferedReader br = new BufferedReader(
				new FileReader(new File("gen/en_wiki_large_abstracts_regexp_lemmat_token_ized.txtOLD")));
		PrintStream psOut = new PrintStream("gen/en_wiki_large_abstracts_regexp_lemmat_token_ized.txt");

		String line;

		while ((line = br.readLine()) != null) {

			String data[] = line.split(">", 2);

			String name = data[0].trim().replaceAll(prefix, "");
			String l = data[1].trim();
			psOut.println(name + "\t" + l);
		}

		psOut.close();
		br.close();
	}

	private static void constructFromRaw() throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(
				new FileReader(new File("/home/hterhors/EnWikipedia/long-abstracts_en.nt")));

		// BufferedReader br = new BufferedReader(
		// new InputStreamReader(new
		// FileInputStream("/home/hterhors/EnWikipedia/long-abstracts_en.nt"),
		// "ASCII"));

		String line;

		PrintStream psOut = new PrintStream("gen/en_wiki_large_abstracts_regexp_lemmat_token_ized.txt");

		StringBuffer curAbstract = new StringBuffer();
		int count = 0;

		List<String> linesToWrite = new ArrayList<String>();

		StanfordLemmatizer lemmatizer = new StanfordLemmatizer();

		while ((line = br.readLine()) != null) {
			count++;
			if (line.startsWith("#"))
				continue;

			line = line.trim();
			line = line.replaceAll("\"@en \\.", "");
			line = line.replaceAll("> \"", "> ");
			line = line.replaceAll("<http://dbpedia\\.org/ontology/abstract>", "");
			curAbstract = new StringBuffer();
			final String name = line.substring(prefix.length(), line.indexOf(">"));

			curAbstract.append(name + "\t");

			line = line.substring(line.indexOf(">") + 1, line.length());

			final String tokenizedDocument = Tokenizer.bagOfWordsTokenizer(line, true, " ");
			List<String> tokenizedLemmatizedDocument = lemmatizer.lemmatizeDocument(tokenizedDocument);

			for (String string : tokenizedLemmatizedDocument) {
				curAbstract.append(string);
				curAbstract.append(" ");
			}

			linesToWrite.add(curAbstract.toString().trim());

			if (count % 1000 == 0) {
				System.out.println("Processed: " + count + " files.");

				linesToWrite.forEach(psOut::println);
				linesToWrite.clear();
			}

		}
		linesToWrite.forEach(psOut::println);
		psOut.close();
		br.close();
	}
}
