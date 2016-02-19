package de.citec.sc.sampling;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.Search;
import de.citec.sc.variables.State;
import sampling.Initializer;

public class GreedyDisambiguationInitializer implements Initializer<Document, State> {

	private static Logger log = LogManager.getFormatterLogger();
	private Search index;

	public GreedyDisambiguationInitializer(Search index) {
		super();
		this.index = index;
	}

	@Override
	public State getInitialState(Document document) {
		log.debug("Initialize State for document:\n%s", document);
		State state = new State(document);
		for (Annotation annotation : document.getGoldResult()) {
			log.debug("Assign initial ID for Annotation:\n%s", annotation);
			List<String> candidateURIs = index.getAllResources(annotation.getWord(), 10);
			if (candidateURIs.isEmpty()) {
				log.warn("No candidates found. Dropping annotation from state.", annotation);
			} else {
				String initialLink = candidateURIs.get(0);
				Annotation newAnnotation = new Annotation(annotation.getWord(), initialLink, annotation.getStartIndex(),
						annotation.getEndIndex(), state.generateEntityID());
				newAnnotation.setIndexRank(0);
				state.addEntity(newAnnotation);
			}
			// initialLink = initialLink.replace("http://dbpedia.org/resource/",
			// "");

		}
		return state;
	}

}
