package de.citec.sc.sampling;

import java.util.ArrayList;
import java.util.List;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.query.Search;
import de.citec.sc.variables.State;
import sampling.Explorer;

/**
 *
 * @author sjebbara
 */
public class DisambiguationExplorer implements Explorer<State> {

	private int maxNumberOfCandidateURIs;
	private Search index;

	public DisambiguationExplorer(Search index) {
		this(index, 100);
	}

	public DisambiguationExplorer(Search index, int maxNumberOfCandidateURIs) {
		super();
		this.index = index;
		this.maxNumberOfCandidateURIs = maxNumberOfCandidateURIs;
	}

	@Override
	public List<State> getNextStates(State currentState) {
		List<State> generatedStates = new ArrayList<>();
		for (Annotation a : currentState.getEntities()) {
			String annotationText = a.getWord();
			// String annotationText =
			// currentState.getDocument().getDocumentContent().substring(a.getStartIndex(),a.getEndIndex());
			List<String> candidateURIs = index.getAllResources(annotationText, maxNumberOfCandidateURIs);
			for (int i = 0; i < candidateURIs.size(); i++) {
				String candidateURI = candidateURIs.get(i);// .replace("http://dbpedia.org/resource/",
															// "");
				State generatedState = new State(currentState);
				Annotation modifiedAnntation = generatedState.getEntity(a.getID());
				modifiedAnntation.setLink(candidateURI);
				modifiedAnntation.setIndexRank(i);
				generatedStates.add(generatedState);
			}
		}
		return generatedStates;
	}

}
