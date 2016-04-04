package de.citec.sc.helper;

import java.util.ArrayList;
import java.util.List;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;

public class DocumentUtils {

	public static List<Document> splitDocument(Document d, int capacity) {
		List<Document> splitted = new ArrayList<>();
		if (d.getGoldStandard().size() > capacity) {

			List<Annotation> resizedGoldStandard = new ArrayList<>();

			for (int j = 0; j < d.getGoldStandard().size(); j++) {
				if (resizedGoldStandard.size() == 50) {
					Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
					d1.setGoldStandard(resizedGoldStandard);
					resizedGoldStandard = new ArrayList<>();

					splitted.add(d1);

					resizedGoldStandard.add(d.getGoldStandard().get(j));

				} else {
					resizedGoldStandard.add(d.getGoldStandard().get(j));
				}
			}

			// add the remaining
			if (resizedGoldStandard.size() <= 20) {
				splitted.get(splitted.size() - 1).getGoldStandard().addAll(resizedGoldStandard);
			} else {
				// create a new document
				Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
				d1.setGoldStandard(resizedGoldStandard);
				resizedGoldStandard = new ArrayList<>();

				splitted.add(d1);

			}
		} else {
			Document d1 = new Document(d.getDocumentContent(), d.getDocumentName());
			d1.setGoldStandard(d.getGoldStandard());

			splitted.add(d1);
		}
		return splitted;
	}
}
