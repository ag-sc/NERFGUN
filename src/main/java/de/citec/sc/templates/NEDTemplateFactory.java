package de.citec.sc.templates;

import de.citec.sc.corpus.Document;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.variables.State;
import exceptions.UnkownTemplateRequestedException;
import templates.AbstractTemplate;
import templates.TemplateFactory;

public class NEDTemplateFactory implements TemplateFactory<Document, State> {

	@Override
	public AbstractTemplate<Document, State, ?> newInstance(String templateName)
			throws UnkownTemplateRequestedException {
		switch (templateName) {
		case "TermFrequencyTemplate":
			return new TermFrequencyTemplate();
		case "PageRankTemplate":
			return new PageRankTemplate();
		case "EditDistanceTemplate":
			return new EditDistanceTemplate();
		case "TopicSpecificPageRankTemplate":
			return new TopicSpecificPageRankTemplate();
		case "DocumentSimilarityTemplate":
			return new DocumentSimilarityTemplate();
		}
		throw new UnkownTemplateRequestedException("Cannot instanciate Template for name " + templateName);
	}

}
