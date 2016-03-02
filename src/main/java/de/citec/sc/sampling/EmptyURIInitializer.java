package de.citec.sc.sampling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import sampling.Initializer;

public class EmptyURIInitializer implements Initializer<Document, State> {

	private static Logger log = LogManager.getFormatterLogger();

	public EmptyURIInitializer() {
		super();
	}

	@Override
	public State getInitialState(Document document) {
		log.debug("Initialize State for document:\n%s", document);
		State state = new State(document);
		for (Annotation annotation : document.getGoldResult()) {
			log.debug("Assign initial ID for Annotation:\n%s", annotation);
			String initialLink = Annotation.DEFAULT_ID;
			Annotation newAnnotation = new Annotation(annotation.getWord(), initialLink, annotation.getStartIndex(),
					annotation.getEndIndex());
			newAnnotation.setIndexRank(-1);
			newAnnotation.setRelativeTermFrequencyScore(-1);
			state.addEntity(newAnnotation);
		}
		return state;
	}

}
