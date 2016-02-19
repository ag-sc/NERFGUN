package de.citec.sc.sampling;

import java.util.List;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.Search;
import de.citec.sc.variables.State;
import sampling.Initializer;

public class GreedyDisambiguationInitializer implements Initializer<Document, State> {

	private Search index;

	public GreedyDisambiguationInitializer(Search index) {
		super();
		this.index = index;
	}

	@Override
	public State getInitialState(Document document) {
		State state = new State(document);
		for (Annotation annotation : document.getGoldResult()) {
			List<String> candidateURIs = index.getAllResources(annotation.getWord(), 10);
			String initialLink = candidateURIs.get(0);
//			initialLink = initialLink.replace("http://dbpedia.org/resource/", "");
			Annotation newAnnotation = new Annotation(annotation.getWord(), initialLink, annotation.getStartIndex(),
					annotation.getEndIndex(), state.generateEntityID());

			newAnnotation.setIndexRank(0);
			state.addEntity(newAnnotation);
		}
		return state;
	}

}
