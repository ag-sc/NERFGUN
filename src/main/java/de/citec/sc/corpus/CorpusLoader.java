/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author sherzod
 */
public class CorpusLoader {
    
    private boolean useOriginal = false;

	private String datasetsPath = "";

//	public CorpusLoader() {
//		datasetsPath += "src/main/resources/";
//	}

	public CorpusLoader(boolean useOriginal) {
            this.useOriginal = useOriginal;
	}

	public enum CorpusName {

		CoNLLTraining, MicroTagging, CoNLLTesta, CoNLLTestb;
	}

	public DefaultCorpus loadCorpus(CorpusName corpusName) {
		DefaultCorpus c = new DefaultCorpus();

		List<Document> documents = new ArrayList<>();

		switch (corpusName) {
		case CoNLLTraining:
			documents = getCONLLDocs("training");
			break;
		case CoNLLTesta:
			documents = getCONLLDocs("testa");
			break;
		case CoNLLTestb:
			documents = getCONLLDocs("testb");
			break;
		case MicroTagging:
			documents = getMicroTaggingDocs();
			break;
		}

		c.addDocuments(documents);

		return c;
	}

	private List<String> readFileAsList(File file) {

		List<String> content = new ArrayList<>();
		try {
			FileInputStream fstream = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				content.add(strLine);
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException("Error reading the file: " + file.getPath() + "\n" + e.getMessage());
		} finally {
		}

		return content;
	}

	private List<Document> getCONLLDocs(String dataset) {
                
		String file = datasetsPath + "dataset/conll/dataset.tsv";
                if(useOriginal){
                    file = datasetsPath + "dataset/conll/datasetOriginal.tsv";
                }

		List<String> list = readFileAsList(new File(file));

		List<Annotation> goldSet = new ArrayList<Annotation>();

		String s = "";

		List<Document> docs = new ArrayList<Document>();

		String docName = "";

		for (String l : list) {
			if (l.startsWith("-DOCSTART-")) {

				if (!s.equals("")) {
					// write out

					Document d1 = new Document(s, docName);
					List<Annotation> annotations = new ArrayList<>();
					for (Annotation ann : goldSet) {
						annotations.add(ann.clone());
					}
					d1.setGoldStandard(annotations);

					if (dataset.equals("training")) {
						if (!d1.getDocumentName().contains("testa") && !d1.getDocumentName().contains("testb")) {
							if (!d1.getGoldStandard().isEmpty()) {
								docs.add(d1);
							}
						}
					} else if (dataset.equals("testa")) {
						if (d1.getDocumentName().contains("testa")) {
							if (!d1.getGoldStandard().isEmpty()) {
								docs.add(d1);
							}
						}
					} else if (dataset.equals("testb")) {
						if (d1.getDocumentName().contains("testb")) {
							if (!d1.getGoldStandard().isEmpty()) {
								docs.add(d1);
							}
						}
					}

					goldSet.clear();
					s = "";
				}
				docName = l;
			} else {
				String[] content = l.split("\t");

				if (content.length == 6) {
					// do B I I
					// European B European Commission European_Commission
					if (content[1].equals("B")) {

						int startIndex = s.length();
						int endIndex = s.length() + content[2].length();

						String label = content[2];

						String uri = content[4].replace("http://en.wikipedia.org/wiki/", "");

						label = StringEscapeUtils.unescapeJava(label);

						try {
							label = URLDecoder.decode(label, "UTF-8");
						} catch (Exception e) {
						}

						uri = StringEscapeUtils.unescapeJava(uri);

						try {
							uri = URLDecoder.decode(uri, "UTF-8");
						} catch (Exception e) {
						}

						Annotation a1 = new Annotation(label, uri, startIndex, endIndex);
						goldSet.add(a1);
						s += content[0] + " ";
					} else {
						s += content[0] + " ";
					}
				}
				if (content.length == 4) {
					// get string
					s += content[0] + " ";
				}
				if (content.length == 1) {
					if (l.equals("")) {
						s += "\n";
					} else {
						s += content[0] + " ";
					}
				}
			}
		}

		if (goldSet.size() > 0 && !s.equals("")) {

			// last doc
			Document d1 = new Document(s, docName);

			List<Annotation> annotations = new ArrayList<>();
			for (Annotation ann : goldSet) {
				annotations.add(ann.clone());
			}
			d1.setGoldStandard(annotations);

			if (dataset.equals("training")) {
				if (!d1.getDocumentName().contains("testa") && !d1.getDocumentName().contains("testb")) {
					if (!d1.getGoldStandard().isEmpty()) {
						docs.add(d1);
					}
				}
			} else if (dataset.equals("testa")) {
				if (d1.getDocumentName().contains("testa")) {
					if (!d1.getGoldStandard().isEmpty()) {
						docs.add(d1);
					}
				}
			} else if (dataset.equals("testb")) {
				if (d1.getDocumentName().contains("testb")) {
					if (!d1.getGoldStandard().isEmpty()) {
						docs.add(d1);
					}
				}
			}

			// docs.add(d1);

			goldSet.clear();
			s = "";
		}

		return docs;
	}

	private List<Document> getMicroTaggingDocs() {
		List<Document> docs = new ArrayList<>();

		List<String> annotations = readFileAsList(new File(datasetsPath + "dataset/microtag/dataset.in"));
		List<String> tweetDocs = readFileAsList(new File(datasetsPath + "dataset/microtag/dataset_tweets.out"));

		HashMap<String, Document> tweetsHashMap = new HashMap();

		for (String s : tweetDocs) {
			String docId = s.split("\t")[0];
			String content = s.replace(docId, "");
			content = content.trim();

			Document d1 = new Document(content, docId);

			tweetsHashMap.put(docId, d1);
		}

		for (String s : annotations) {
			String docId = s.split("\t")[0];
			String ans = s.replace(docId, "");
			ans = ans.trim();

			if (!ans.equals("")) {

				String[] arrayOfAnnotations = ans.split("\t");
				List<Annotation> goldSet = new ArrayList<>();

				for (int i = 0; i < arrayOfAnnotations.length; i = i + 2) {

					String label = arrayOfAnnotations[i];
					String uri = arrayOfAnnotations[i + 1].replace("http://dbpedia.org/resource/", "");
					label = StringEscapeUtils.unescapeJava(label);
					uri = StringEscapeUtils.unescapeJava(uri);
					try {
						uri = URLDecoder.decode(uri, "UTF-8");
					} catch (Exception e) {

					}

					try {
						label = URLDecoder.decode(label, "UTF-8");
					} catch (Exception e) {

					}
					Annotation a1 = new Annotation(label, uri, 0, 0);
					goldSet.add(a1);
				}

				if (tweetsHashMap.containsKey(docId)) {
					Document d = tweetsHashMap.get(docId);

					d.setGoldStandard(goldSet);

					docs.add(d);
				}
			}

		}

		return docs;
	}
}