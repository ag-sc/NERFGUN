package de.citec.sc.sampling;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.Instance;
import de.citec.sc.variables.State;
import sampling.Initializer;

public class DisambiguationInitializer implements Initializer<Document, State> {

	private static Logger log = LogManager.getFormatterLogger();
	private CandidateRetriever index;
	private boolean assignRandomURI;

	private Random random;
	private int topK;

	/**
	 * This constructor creates an Initializer that either assigns the topmost
	 * candidate uri or a random uri of the respective candidate uris to each
	 * annotation.
	 * 
	 * @param index
	 * @param assignRandomURI
	 */
	public DisambiguationInitializer(CandidateRetriever index, int topK, boolean assignRandomURI) {
		super();
		this.index = index;
		this.assignRandomURI = assignRandomURI;
		this.random = new Random(100l);
		this.topK = topK;
	}

	/**
	 * This constructor creates an Initializer that assigns the topmost
	 * candidate uri to each annotation.
	 * 
	 * @param index
	 */
	public DisambiguationInitializer(CandidateRetriever index, int topK) {
		this(index, topK, false);
	}

	@Override
	public State getInitialState(Document document) {
		log.debug("Initialize State for document:\n%s", document);
		State state = new State(document);
		for (Annotation annotation : document.getGoldResult()) {
			log.debug("Assign initial ID for Annotation:\n%s", annotation);
			List<Instance> candidateURIs = index.getAllResources(annotation.getWord(), topK);
			if (candidateURIs.isEmpty()) {
				log.warn("No candidates found for %s. Dropping annotation from state.", annotation);
			} else {
				int candidateRank;
				if (assignRandomURI) {
					candidateRank = (int) (random.nextDouble() * candidateURIs.size());
				} else {
					candidateRank = 0;
				}

				Instance candidate = candidateURIs.get(candidateRank);
				String initialLink = candidate.getUri();
				Annotation newAnnotation = new Annotation(annotation.getWord(), initialLink, annotation.getStartIndex(),
						annotation.getEndIndex());

				newAnnotation.setIndexRank(candidateRank);
				newAnnotation.setRelativeTermFrequencyScore(candidate.getScore());
				state.addEntity(newAnnotation);
			}
			// initialLink = initialLink.replace("http://dbpedia.org/resource/",
			// "");

		}
		return state;
	}

}
