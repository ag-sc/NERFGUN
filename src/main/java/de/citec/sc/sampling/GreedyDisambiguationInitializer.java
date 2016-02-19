package de.citec.sc.sampling;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.Instance;
import de.citec.sc.variables.State;
import java.util.List;
import java.util.List;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import sampling.Initializer;



public class GreedyDisambiguationInitializer implements Initializer<Document, State> {

    
	private CandidateRetriever index;

    public GreedyDisambiguationInitializer(CandidateRetriever index) {

        super();
        this.index = index;
    }

    @Override
    public State getInitialState(Document document) {
        State state = new State(document);
        for (Annotation annotation : document.getGoldResult()) {

            List<Instance> candidateURIs = index.getAllResources(annotation.getWord(), 10);
            String initialLink = candidateURIs.get(0).getUri();
//			initialLink = initialLink.replace("http://dbpedia.org/resource/", "");
            Annotation newAnnotation = new Annotation(annotation.getWord(), initialLink, annotation.getStartIndex(),
                    annotation.getEndIndex(), state.generateEntityID());

            newAnnotation.setIndexRank(0);
            state.addEntity(newAnnotation);
        }
        return state;
    }

}
