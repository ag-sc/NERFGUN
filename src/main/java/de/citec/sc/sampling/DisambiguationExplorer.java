package de.citec.sc.sampling;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.Instance;
import de.citec.sc.variables.State;
import sampling.Explorer;

/**
 *
 * @author sjebbara
 */
public class DisambiguationExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger();
	private int maxNumberOfCandidateURIs;
	private CandidateRetriever index;

	public DisambiguationExplorer(CandidateRetriever index) {
		this(index, 100);
	}

	public DisambiguationExplorer(CandidateRetriever index, int maxNumberOfCandidateURIs) {
		super();
		this.index = index;
		this.maxNumberOfCandidateURIs = maxNumberOfCandidateURIs;
	}

	@Override
	public List<State> getNextStates(State currentState) {
		log.debug("Generate successor states for state:\n%s", currentState);
		List<State> generatedStates = new ArrayList<>();
		for (Annotation a : currentState.getEntities()) {
			log.debug("Generate successor states for annotation:\n%s", a);
			String annotationText = a.getWord();
			// String annotationText =
			// currentState.getDocument().getDocumentContent().substring(a.getStartIndex(),a.getEndIndex());
			List<Instance> candidateURIs = index.getAllResources(annotationText, maxNumberOfCandidateURIs);
			log.debug("%s candidates retreived.", candidateURIs.size());
			for (int i = 0; i < candidateURIs.size(); i++) {
				Instance candidateURI = candidateURIs.get(i);// .replace("http://dbpedia.org/resource/",
																// "");
				State generatedState = new State(currentState);
				Annotation modifiedAnntation = generatedState.getEntity(a.getID());
				modifiedAnntation.setLink(candidateURI.getUri());
				modifiedAnntation.setIndexRank(i);
				generatedStates.add(generatedState);
			}
		}
		log.debug("Total number of %s states generated.", generatedStates.size());
		return generatedStates;
	}

}
