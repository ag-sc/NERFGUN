package de.citec.sc.templates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import exceptions.UnkownTemplateRequestedException;
import templates.AbstractTemplate;
import templates.TemplateFactory;

public class NEDTemplateFactory implements TemplateFactory<Document, State> {

    private boolean useBins = true;

    public NEDTemplateFactory(boolean b) {
        this.useBins = b;
    }

    @Override
    public AbstractTemplate<Document, State, ?> newInstance(String templateName)
            throws UnkownTemplateRequestedException, Exception {

        boolean binsForPRTF = true;

        switch (templateName) {
            case "TermFrequencyTemplate":
                return new TermFrequencyTemplate(binsForPRTF);
            case "PageRankTemplate":
                return new PageRankTemplate(binsForPRTF);
            case "EditDistanceTemplate":
                return new EditDistanceTemplate(useBins);
            case "TopicSpecificPageRankTemplate":
                return new TopicSpecificPageRankTemplate(useBins);
            case "DocumentSimilarityTemplate":
                return new DocumentSimilarityTemplate();
            case "CandidateSimilarityTemplate":
                return new CandidateSimilarityTemplate();
            case "ClassContextTemplate":
                return new ClassContextTemplate();
            case "CategoryTemplate":
                return new CategoryTemplate(false);
            case "PairwiseClassOccurenceTemplate":
                return new PairwiseClassOccurenceTemplate();
            case "NameSurnameTemplate":
                return new NameSurnameTemplate();
            case "LocalIDFDocumentSimilarityTemplate":
                return new LocalIDFDocumentSimilarityTemplate();
        }
        throw new UnkownTemplateRequestedException("Cannot instanciate Template for name " + templateName);
    }

}
