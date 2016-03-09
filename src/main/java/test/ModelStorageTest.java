package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collections;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnLucene;
import de.citec.sc.sampling.AllScoresExplorer;
import de.citec.sc.sampling.DisambiguationInitializer;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.InitializationException;
import de.citec.sc.templates.NEDTemplateFactory;
import de.citec.sc.templates.TermFrequencyTemplate;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import de.citec.sc.variables.State;
import evaluation.EvaluationUtil;
import exceptions.UnkownTemplateRequestedException;
import learning.DefaultLearner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Trainer;
import learning.scorer.DefaultScorer;
import learning.scorer.Scorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.stoppingcriterion.StoppingCriterion;
import templates.AbstractTemplate;
import templates.TemplateFactory;

/*
 * 	templates.add(new EditDistanceTemplate());
 *	templates.add(new TopicSpecificPageRankTemplate(tsprIndexMappingFile, tsprFile));
 *
 */
//02:52:23.144 [main] INFO  - Micro-average Precision=0.5445
//02:52:23.144 [main] INFO  - Micro-average Recall=0.5445
//02:52:23.144 [main] INFO  - F1 Micro-average=0.5445
//02:52:23.144 [main] INFO  - Macro-average Precision=0.5525
//02:52:23.145 [main] INFO  - Macro-average Recall=0.5525
//02:52:23.145 [main] INFO  - F1 Macro-average=0.5525
public class ModelStorageTest {
	private static Logger log = LogManager.getFormatterLogger();

	public static void main(String[] args) throws IOException {
		ModelStorageTest bire = new ModelStorageTest();
		// bire.run();
		try {
			bire.testModelStorage();
			// bire.testModelLoading();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testModelStorage() throws IOException, InitializationException {

		String indexFile = "tfidf.bin";
		String dfFile = "en_wiki_large_abstracts.docfrequency";
		String tfidfFile = "en_wiki_large_abstracts.tfidf";
		String tsprFile = "tspr.gold";
		String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

		log.info("Init TopicSpecificPageRankTemplate ...");
		TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);
		log.info("Init DocumentSimilarityTemplate ...");
		// DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
		/*
		 * Load the index API.
		 */
		log.info("Load Index...");
		CandidateRetriever index = new CandidateRetrieverOnLucene(false, "mergedIndex");
		// CandidateRetriever index = new CandidateRetrieverOnMemory();

		// Search index = new SearchCache(false, "dbpediaIndexAll");
		/*
		 * Load training and test data.
		 */
		log.info("Load Corpus...");
		CorpusLoader loader = new CorpusLoader();
		DefaultCorpus trainingCorpus = loader.loadCorpus(CorpusName.CoNLLTraining);
		DefaultCorpus testaCorpus = loader.loadCorpus(CorpusName.CoNLLTesta);
		List<Document> documents = trainingCorpus.getDocuments();

		AllScoresExplorer exp1 = new AllScoresExplorer(index);

		List<Document> train = trainingCorpus.getDocuments();
		List<Document> test = testaCorpus.getDocuments();

		log.info("Train data:");
		train.forEach(s -> log.info("%s", s));

		log.info("Test data:");
		test.forEach(s -> log.info("%s", s));
		/*
		 * In the following, we setup all necessary components for training and
		 * testing.
		 */
		/*
		 * Define an objective function that guides the training procedure.
		 */
		ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

		/*
		 * Define templates that are responsible to generate factors/features to
		 * score intermediate, generated states.
		 */
		List<AbstractTemplate<Document, State, ?>> templates = new ArrayList<>();
		TermFrequencyTemplate lTemplate = new TermFrequencyTemplate();
		// PageRankTemplate pTemplate = new PageRankTemplate();
		EditDistanceTemplate eTemplate = new EditDistanceTemplate();
		TopicSpecificPageRankTemplate tpTemplate = new TopicSpecificPageRankTemplate();
		// DocumentSimilarityTemplate dTemplate = new
		// DocumentSimilarityTemplate();

		templates.add(lTemplate);
		// templates.add(pTemplate);
		// templates.add(rankTemplate);
		templates.add(eTemplate);
		// templates.add(tpTemplate);
		// templates.add(dTemplate);

		/*
		 * Define a model and provide it with the necessary templates.
		 */
		Model<Document, State> model = new Model<>(templates);
		model.setMultiThreaded(true);
		/*
		 * Create the scorer object that computes a score from the features of a
		 * factor and the weight vectors of the templates.
		 */
		Scorer scorer = new DefaultScorer();

		/*
		 * Create an Initializer that is responsible for providing an initial
		 * state for the sampling chain given a sentence.
		 */
		Initializer<Document, State> initializer = new DisambiguationInitializer(index, true);

		/*
		 * Define the explorers that will provide "neighboring" states given a
		 * starting state. The sampler will select one of these states as a
		 * successor state and, thus, perform the sampling procedure.
		 */
		List<Explorer<State>> explorers = new ArrayList<>();
		// explorers.add(new DisambiguationExplorer(index));
		explorers.add(exp1);
		/*
		 * Create a sampler that generates sampling chains with which it will
		 * trigger weight updates during training.
		 */

		/*
		 * If you set this value too small, the sampler can not reach the
		 * optimal solution. Large values, however, increase computation time.
		 */
		int numberOfSamplingSteps = 200;

		/*
		 * Stop sampling if objective score is equal to 1.
		 */
		StoppingCriterion<State> objectiveOneCriterion = new StoppingCriterion<State>() {

			@Override
			public boolean checkCondition(List<State> chain, int step) {
				if (chain.isEmpty())
					return false;

				double maxScore = chain.get(chain.size() - 1).getModelScore();
				if (maxScore >= 1)
					return true;
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

		// StoppingCriterion<State> stoppingCriterion = new
		// StepLimitCriterion<>(numberOfSamplingSteps);
		DefaultSampler<Document, State, List<Annotation>> sampler = new DefaultSampler<>(model, scorer, objective,
				explorers, objectiveOneCriterion);
		sampler.setSamplingStrategy(SamplingStrategies.greedyObjectiveStrategy());
		sampler.setAcceptStrategy(AcceptStrategies.strictObjectiveAccept());
		/*
		 * Define a learning strategy. The learner will receive state pairs
		 * which can be used to update the models parameters.
		 */
		DefaultLearner<State> learner = new DefaultLearner<>(model, 0.1);

		log.info("####################");
		log.info("Start training");

		/*
		 * The trainer will loop over the data and invoke sampling and learning.
		 * Additionally, it can invoke predictions on new data.
		 */
		int numberOfEpochs = 1;
		Trainer trainer = new Trainer();
		trainer.train(sampler, initializer, learner, train, numberOfEpochs);

		/*
		 * Stop sampling if model score does not increase for 5 iterations.
		 */
		StoppingCriterion<State> stopAtMaxModelScore = new StoppingCriterion<State>() {

			@Override
			public boolean checkCondition(List<State> chain, int step) {

				if (chain.isEmpty())
					return false;

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

		sampler.setStoppingCriterion(stopAtMaxModelScore);

		/*
		 * Perform prediction on training and test data.
		 */
		List<State> trainResults = trainer.test(sampler, initializer, train);

		/*
		 * Give the final annotations to the Document for the Evaluator
		 */
		for (State state : trainResults) {
			state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
		}
		/*
		 * Evaluate train and test predictions
		 */
		Map<String, Double> trainEvaluation = Evaluator.evaluateAll(train);

		/*
		 * Print evaluation
		 */
		log.info("Evaluation on training data:");
		trainEvaluation.entrySet().forEach(e -> log.info(e));

		/*
		 * Finally, print the models weights.
		 */
		log.info("Model weights:");
		EvaluationUtil.printWeights(model, 0);

		/*
		 * Same for testdata
		 */

		List<State> testResults = trainer.test(sampler, initializer, test);
		for (State state : testResults) {
			state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
		}
		Map<String, Double> testEvaluation = Evaluator.evaluateAll(test);
		log.info("Evaluation on test data:");
		testEvaluation.entrySet().forEach(e -> log.info(e));
		/*
		 * Finally, print the models weights.
		 */
		log.info("Model weights:");
		EvaluationUtil.printWeights(model, 0);

		model.saveModelToFile("src/main/resources/models", "model1Test");

	}

	public void testModelLoading() throws IOException, ClassNotFoundException, UnkownTemplateRequestedException {

		String indexFile = "tfidf.bin";
		String dfFile = "en_wiki_large_abstracts.docfrequency";
		String tfidfFile = "en_wiki_large_abstracts.tfidf";
		String tsprFile = "tspr.gold";
		String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

		log.info("Init TopicSpecificPageRankTemplate ...");
		TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);
		log.info("Init DocumentSimilarityTemplate ...");
		// DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
		/*
		 * Load the index API.
		 */
		log.info("Load Index...");
		CandidateRetriever index = new CandidateRetrieverOnLucene(false, "mergedIndex");
		// CandidateRetriever index = new CandidateRetrieverOnMemory();

		// Search index = new SearchCache(false, "dbpediaIndexAll");
		/*
		 * Load training and test data.
		 */
		log.info("Load Corpus...");
		CorpusLoader loader = new CorpusLoader();
		DefaultCorpus trainingCorpus = loader.loadCorpus(CorpusName.CoNLLTraining);
		DefaultCorpus testaCorpus = loader.loadCorpus(CorpusName.CoNLLTesta);
		List<Document> documents = trainingCorpus.getDocuments();

		AllScoresExplorer exp1 = new AllScoresExplorer(index);

		List<Document> train = trainingCorpus.getDocuments();
		List<Document> test = testaCorpus.getDocuments();

		log.info("Train data:");
		train.forEach(s -> log.info("%s", s));

		log.info("Test data:");
		test.forEach(s -> log.info("%s", s));
		/*
		 * In the following, we setup all necessary components for training and
		 * testing.
		 */
		/*
		 * Define an objective function that guides the training procedure.
		 */
		ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

		/*
		 * Define templates that are responsible to generate factors/features to
		 * score intermediate, generated states.
		 */

		TemplateFactory<Document, State> templateFactory = new NEDTemplateFactory();
		/*
		 * Define a model and provide it with the necessary templates.
		 */
		Model<Document, State> model = new Model<>();
		model.setMultiThreaded(true);
		try {
			model.loadModelFromDir(new File("src/main/resources/models", "model1Test"), templateFactory);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		/*
		 * Create the scorer object that computes a score from the features of a
		 * factor and the weight vectors of the templates.
		 */
		Scorer scorer = new DefaultScorer();

		/*
		 * Create an Initializer that is responsible for providing an initial
		 * state for the sampling chain given a sentence.
		 */
		Initializer<Document, State> initializer = new DisambiguationInitializer(index, true);

		/*
		 * Define the explorers that will provide "neighboring" states given a
		 * starting state. The sampler will select one of these states as a
		 * successor state and, thus, perform the sampling procedure.
		 */
		List<Explorer<State>> explorers = new ArrayList<>();
		// explorers.add(new DisambiguationExplorer(index));
		explorers.add(exp1);
		/*
		 * Create a sampler that generates sampling chains with which it will
		 * trigger weight updates during training.
		 */

		/*
		 * If you set this value too small, the sampler can not reach the
		 * optimal solution. Large values, however, increase computation time.
		 */
		int numberOfSamplingSteps = 200;
		StoppingCriterion<State> stopAtMaxModelScore = new StoppingCriterion<State>() {

			@Override
			public boolean checkCondition(List<State> chain, int step) {

				if (chain.isEmpty())
					return false;

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
		/*
		 * Stop sampling if objective score is equal to 1.
		 */

		DefaultSampler<Document, State, List<Annotation>> sampler = new DefaultSampler<>(model, scorer, objective,
				explorers, stopAtMaxModelScore);
		sampler.setSamplingStrategy(SamplingStrategies.greedyObjectiveStrategy());
		sampler.setAcceptStrategy(AcceptStrategies.strictObjectiveAccept());
		log.info("####################");
		log.info("Start training");

		/*
		 * The trainer will loop over the data and invoke sampling and learning.
		 * Additionally, it can invoke predictions on new data.
		 */
		int numberOfEpochs = 1;
		Trainer trainer = new Trainer();

		/*
		 * Stop sampling if model score does not increase for 5 iterations.
		 */

		/*
		 * Perform prediction on training and test data.
		 */
		List<State> trainResults = trainer.test(sampler, initializer, train);

		/*
		 * Give the final annotations to the Document for the Evaluator
		 */
		for (State state : trainResults) {
			state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
		}
		/*
		 * Evaluate train and test predictions
		 */
		Map<String, Double> trainEvaluation = Evaluator.evaluateAll(train);

		/*
		 * Print evaluation
		 */
		log.info("Evaluation on training data:");
		trainEvaluation.entrySet().forEach(e -> log.info(e));

		/*
		 * Finally, print the models weights.
		 */
		log.info("Model weights:");
		EvaluationUtil.printWeights(model, 0);

		/*
		 * Same for testdata
		 */

		List<State> testResults = trainer.test(sampler, initializer, test);
		for (State state : testResults) {
			state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
		}
		Map<String, Double> testEvaluation = Evaluator.evaluateAll(test);
		log.info("Evaluation on test data:");
		testEvaluation.entrySet().forEach(e -> log.info(e));
		/*
		 * Finally, print the models weights.
		 */
		log.info("Model weights:");
		EvaluationUtil.printWeights(model, 0);

	}

}
