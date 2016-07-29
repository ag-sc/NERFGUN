package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;

public class InspectDataset {

//	public static void main(String[] args) {
//		CorpusLoader loader = new CorpusLoader();
//
//		DefaultCorpus corpus = loader.loadCorpus(CorpusName.CoNLLTraining);
//		List<Document> documents = corpus.getDocuments();
//
//		int tp = 0;
//		int fp = 0;
//		for (Document d : documents) {
//			// System.out.println();
//			// System.out.println();
//			// System.out.println("###############################################");
//			// System.out.println(d.getDocumentName());
//			// Map<String, Annotation> mentions = new HashMap<>();
//			// for (Annotation a : d.getGoldStandard()) {
//			// mentions.put(a.getWord().toLowerCase(), a);
//			// }
//
//			for (Annotation a1 : d.getGoldStandard()) {
//				for (Annotation a2 : d.getGoldStandard()) {
//					if (a1.getWord().length() < a2.getWord().length()) {
//						// System.out.println(String.format("%s < %s",
//						// a1.getWord() ,a2.getWord() ));
//						// System.out.println(String.format("%s same URI
//						// %s", a1.getWord() ,a2.getWord() ));
//						if (isSurnameOf(a1.getWord().toLowerCase(), a2.getWord().toLowerCase())) {
//							if (!a1.equals(a2) && a1.getLink().equals(a2.getLink())) {
//								// System.out.println(String.format("%s is in
//								// %s",
//								// a1.getWord(), a2.getWord()));
//								tp++;
//								break;
//							} else {
//								System.out.println(String.format("WRONG: %s [%s]    is    in %s [%s]", a1.getWord(),
//										a1.getLink(), a2.getWord(), a2.getLink()));
//								fp++;
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
//		System.out.println();
//		System.out.println();
//		System.out.println("TP: " + tp);
//		System.out.println("FP: " + fp);
//		// System.out.println(String.format("%s/%s=%s", c, a2.getWord()));
//	}
	
	public static void main(String[] args) {
		CorpusLoader loader = new CorpusLoader(false);
		
		System.out.println("CoNLLTraining");
		DefaultCorpus corpus = loader.loadCorpus(CorpusName.CoNLLTraining);
		System.out.println(corpus.getDocuments().size());
		System.out.println("CoNLLTesta");
		loader = new CorpusLoader(false);
		corpus = loader.loadCorpus(CorpusName.CoNLLTesta);
		System.out.println(corpus.getDocuments().size());
		System.out.println("CoNLLTestb");
		loader = new CorpusLoader(false);
		corpus = loader.loadCorpus(CorpusName.CoNLLTestb);
		System.out.println(corpus.getDocuments().size());
		
		List<Document> documents = corpus.getDocuments();
		
		int tp = 0;
		int fp = 0;
		for (Document d : documents) {
			// System.out.println();
			// System.out.println();
			// System.out.println("###############################################");
			// System.out.println(d.getDocumentName());
			// Map<String, Annotation> mentions = new HashMap<>();
			// for (Annotation a : d.getGoldStandard()) {
			// mentions.put(a.getWord().toLowerCase(), a);
			// }
			
			for (Annotation a1 : d.getGoldStandard()) {
				for (Annotation a2 : d.getGoldStandard()) {
					if (a1.getWord().length() < a2.getWord().length()) {
						// System.out.println(String.format("%s < %s",
						// a1.getWord() ,a2.getWord() ));
						// System.out.println(String.format("%s same URI
						// %s", a1.getWord() ,a2.getWord() ));
						if (isSurnameOf(a1.getWord().toLowerCase(), a2.getWord().toLowerCase())) {
							if (!a1.equals(a2) && a1.getLink().equals(a2.getLink())) {
								// System.out.println(String.format("%s is in
								// %s",
								// a1.getWord(), a2.getWord()));
								tp++;
								break;
							} else {
								System.out.println(String.format("WRONG: %s [%s]    is    in %s [%s]", a1.getWord(),
										a1.getLink(), a2.getWord(), a2.getLink()));
								fp++;
								break;
							}
						}
					}
				}
			}
		}
		System.out.println();
		System.out.println();
		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
	}

	public static boolean isSurnameOf(String surname, String fullname) {
		String[] sParts = surname.split(" ");
		String[] fParts = fullname.split(" ");
		if (sParts.length == 1 && (fParts.length >= 2 && fParts.length <= 3)) {
			return fullname.endsWith(surname);
		}
		return false;
	}
}
