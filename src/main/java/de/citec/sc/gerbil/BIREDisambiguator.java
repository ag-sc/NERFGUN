package de.citec.sc.gerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.DocumentUtils;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.sampling.AllScoresExplorer;
import de.citec.sc.sampling.DisambiguationInitializer;
import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.templates.NEDTemplateFactory;
import de.citec.sc.templates.PageRankTemplate;
import de.citec.sc.templates.TermFrequencyTemplate;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import de.citec.sc.variables.State;
import exceptions.UnkownTemplateRequestedException;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Trainer;
import learning.scorer.LinearScorer;
import learning.scorer.Scorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.stoppingcriterion.StoppingCriterion;
import spark.ResponseTransformer;
import spark.Spark;
import templates.AbstractTemplate;
import templates.TemplateFactory;

public class BIREDisambiguator implements TemplateFactory<Document, State> {
	private static Logger log = LogManager.getFormatterLogger();

	private static String indexFile = "tfidf.bin";
	private static String dfFile = "en_wiki_large_abstracts.docfrequency";
	private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
	private static String tsprFile = "tspr.gold";
	private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";
	private static int MAX_CANDIDATES = 100;
	private int numberOfSamplingSteps = 100;
	private boolean useBins = true;
	private int capacity = 70;

	private File modelDir;
	private CandidateRetriever index;
	private ObjectiveFunction<State, List<Annotation>> objective;
	private NEDTemplateFactory factory;
	private Scorer scorer;
	private Model<Document, State> model;
	private Initializer<Document, State> testInitializer;
	private List<Explorer<State>> explorers;
	private StoppingCriterion<State> stopAtMaxModelScore;
	private DefaultSampler<Document, State, List<Annotation>> sampler;
	private Trainer trainer;

	public BIREDisambiguator() {
	}

	public void init(String modelDirPath)
			throws FileNotFoundException, IOException, UnkownTemplateRequestedException, Exception {
		modelDir = new File(modelDirPath);
		index = new CandidateRetrieverOnMemory();

		objective = new DisambiguationObjectiveFunction();
		scorer = new LinearScorer();

		model = new Model<>(scorer);
		model.setForceFactorComputation(false);
		model.setMultiThreaded(true);
		model.loadModelFromDir(modelDir, factory);

		testInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);

		explorers = new ArrayList<>();
		explorers.add(new AllScoresExplorer(index, MAX_CANDIDATES));
		numberOfSamplingSteps = 200;

		stopAtMaxModelScore = new StoppingCriterion<State>() {

			@Override
			public boolean checkCondition(List<State> chain, int step) {

				if (chain.isEmpty()) {
					return false;
				}

				double maxScore = chain.get(chain.size() - 1).getModelScore();
				int count = 0;
				final int maxCount = 5;

				for (int i = 0; i < chain.size(); i++) {
					if (chain.get(i).getModelScore() >= maxScore) {
						count++;
					}
				}
				return count >= maxCount || step >= numberOfSamplingSteps;
			}
		};
		sampler = new DefaultSampler<>(model, objective, explorers, stopAtMaxModelScore);

		sampler.setSamplingStrategy(SamplingStrategies.greedyModelStrategy());
		sampler.setAcceptStrategy(AcceptStrategies.strictModelAccept());
		trainer = new Trainer();
	}

	public void run() {
		Spark.post("/ned/json", "application/json", (request, response) -> {
			String jsonDocument = request.body();
			Document document = GerbilUtil.json2bire(jsonDocument);
			Document annotatedDocument = disambiguate(document);

			response.type("application/json");
			return GerbilUtil.bire2json(annotatedDocument);
		});
	}

	public Document disambiguate(Document document) {
		log.info("####################");
		log.info("Request to disambiguate document:\n%s", document);

		List<Document> test = DocumentUtils.splitDocument(document, capacity);
		log.info("Split into %s smaller documents.", test.size());

		log.info("Predict ...");
		List<State> testResults = trainer.predict(sampler, testInitializer, test);

		Document annotatedDocument = new Document(document.getDocumentContent(), document.getDocumentName());
		for (State state : testResults) {
			List<Annotation> a = annotatedDocument.getAnnotations();
			a.addAll(state.getEntities());
		}
		log.info("Done!");

		log.info("---------------------");
		log.info("Disambiguated document:\n%s", annotatedDocument);
		return annotatedDocument;
	}

	@Override
	public AbstractTemplate<Document, State, ?> newInstance(String templateName)
			throws UnkownTemplateRequestedException, Exception {
		switch (templateName) {
		case "TermFrequencyTemplate":
			return new TermFrequencyTemplate();
		case "PageRankTemplate":
			return new PageRankTemplate();
		case "EditDistanceTemplate":
			return new EditDistanceTemplate(useBins);
		case "TopicSpecificPageRankTemplate":
			log.info("Init TopicSpecificPageRankTemplate ...");
			TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);
			IndexMapping.init(tsprIndexMappingFile);
			return new TopicSpecificPageRankTemplate(useBins);
		case "DocumentSimilarityTemplate":
			log.info("Init DocumentSimilarityTemplate ...");
			DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
			return new DocumentSimilarityTemplate();
		}
		throw new UnkownTemplateRequestedException("Cannot instanciate Template for name " + templateName);
	}

}
