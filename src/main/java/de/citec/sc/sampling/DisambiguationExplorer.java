package de.citec.sc.sampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
	private Multimap<String, String> candidateCache = HashMultimap.create();
	private boolean cacheCandidates;

	public DisambiguationExplorer(Search index) {
		this(index, 100, false);
	}

	public DisambiguationExplorer(Search index, int maxNumberOfCandidateURIs, boolean cacheCandidates) {
		super();
		this.index = index;
		this.maxNumberOfCandidateURIs = maxNumberOfCandidateURIs;
		this.cacheCandidates = cacheCandidates;
	}

	@Override
	public List<State> getNextStates(State currentState) {
		List<State> generatedStates = new ArrayList<>();
		for (Annotation a : currentState.getEntities()) {
			String annotationText = a.getWord();
			// String annotationText =
			// currentState.getDocument().getDocumentContent().substring(a.getStartIndex(),a.getEndIndex());
			Collection<String> candidateURIs;
			if (cacheCandidates) {
				if (candidateCache.containsKey(annotationText)) {
					candidateURIs = candidateCache.get(annotationText);
				} else {
					candidateURIs = index.getAllResources(annotationText, maxNumberOfCandidateURIs);
					candidateCache.putAll(annotationText, candidateURIs);
				}
			} else {
				candidateURIs = index.getAllResources(annotationText, maxNumberOfCandidateURIs);
			}

			for (String candidateURI : candidateURIs) {
				State generatedState = new State(currentState);
				generatedState.getEntity(a.getID()).setLink(candidateURI);
				generatedStates.add(generatedState);
			}
		}
		return generatedStates;
	}

}
