package de.citec.sc;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnLucene;
import de.citec.sc.sampling.DisambiguationExplorer;
import de.citec.sc.sampling.EmptyURIInitializer;
import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.LuceneScoreTemplate;
import de.citec.sc.templates.PageRankTemplate;
import de.citec.sc.variables.State;
import evaluation.EvaluationUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import learning.DefaultLearner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.scorer.DefaultScorer;
import learning.Trainer;
import learning.scorer.DefaultScorer;
import learning.scorer.Scorer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.stoppingcriterion.StoppingCriterion;
import templates.AbstractTemplate;

public class BIREMain {
	private static Logger log = LogManager.getFormatterLogger();

	public static void main(String[] args) {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.INFO);
		ctx.updateLoggers();

		String indexFile = "tfidf.bin";
		String dfFile = "en_wiki_large_abstracts.docfrequency";
		String tfidfFile = "en_wiki_large_abstracts.tfidf";
		String tsprFile = "tspr.gold";
		String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

		/*
		 * Load the index API.
		 */
		log.info("Load Index...");
		CandidateRetriever index = new CandidateRetrieverOnLucene(true, "dbpediaIndex", "anchorIndex");

		// Search index = new SearchCache(false, "dbpediaIndexAll");
		/*
		 * Load training and test data.
		 */
		log.info("Load Corpus...");
		CorpusLoader loader = new CorpusLoader();
		DefaultCorpus corpus = loader.loadCorpus(CorpusName.CoNLL);
		List<Document> documents = corpus.getDocuments();

		documents = documents.subList(0, 10);

		/*
		 * Some code for n-fold cross validation
		 */
		Map<String, Double> avrgTrain = new LinkedHashMap<>();
		Map<String, Double> avrgTest = new LinkedHashMap<>();
		Collections.shuffle(documents);
		int N = documents.size();
		int n = 2;
		double step = ((float) N) / n;
		double k = 0;
                PageRankTemplate pTemplate = new PageRankTemplate();
                LuceneScoreTemplate lTemplate = new LuceneScoreTemplate(index);
//                IndexRankTemplate rankTemplate = new IndexRankTemplate();
		for (int i = 0; i < n; i++) {
			log.info("Cross-Validation Fold %s/%s", i + 1, n);
			double j = k;
			k = j + step;

			// List<Document> test = documents;(IOException e1) {
			// e1.printStackTrace();
			// System.exit(1);
			// }
			// List<Document> train = documents;
			List<Document> test = documents.subList((int) Math.floor(j), (int) Math.floor(k));
			List<Document> train = new ArrayList<>(documents);
			train.removeAll(test);

			log.info("Train data:");
			train.forEach(s -> log.info("%s", s));

			log.info("Test data:");
			test.forEach(s -> log.info("%s", s));
			/*
			 * In the following, we setup all necessary components for training
			 * and testing.
			 */
			/*
			 * Define an objective function that guides the training procedure.
			 */
			ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

			/*
			 * Define templates that are responsible to generate
			 * factors/features to score intermediate, generated states.
			 */
			List<AbstractTemplate<State>> templates = new ArrayList<>();
			// templates.add(new IndexRankTemplate());
                        
			try {
//                            templates.add(rankTemplate);
                            templates.add(pTemplate);
                            
                            templates.add(lTemplate);
				 templates.add(new EditDistanceTemplate());
//				templates.add(new TopicSpecificPageRankTemplate(tsprIndexMappingFile, tsprFile));
				// templates.add(new DocumentSimilarityTemplate(indexFile,
				// tfidfFile, dfFile, true));
			} catch (Exception e1) {
				e1.printStackTrace();
				System.exit(1);
			}

			// templates.add(new PageRankTemplate());

			/*
			 * Define a model and provide it with the necessary templates.
			 */
			Model<State> model = new Model<>(templates);
			/*
			 * Create the scorer object that computes a score from the features
			 * of a factor and the weight vectors of the templates.
			 */
			Scorer<State> scorer = new DefaultScorer<>();

			/*
			 * Create an Initializer that is responsible for providing an
			 * initial state for the sampling chain given a sentence.
			 */
			Initializer<Document, State> initializer = new EmptyURIInitializer();

			/*
			 * Define the explorers that will provide "neighboring" states given
			 * a starting state. The sampler will select one of these states as
			 * a successor state and, thus, perform the sampling procedure.
			 */
			List<Explorer<State>> explorers = new ArrayList<>();
			explorers.add(new DisambiguationExplorer(index));
			/*
			 * Create a sampler that generates sampling chains with which it
			 * will trigger weight updates during training.
			 */

			/*
			 * If you set this value too small, the sampler can not reach the
			 * optimal solution. Large values, however, increase computation
			 * time.
			 */
			int numberOfSamplingSteps = 200;

			/*
			 * Stop sampling if objective score is equal to 1.
			 */
			StoppingCriterion<State> objectiveOne = new StoppingCriterion<State>() {

				@Override
				public boolean checkCondition(List<State> chain, int step) {
					return !chain.isEmpty() && chain.get(chain.size() - 1).getObjectiveScore() >= 1
							|| step >= numberOfSamplingSteps;
				}
			};

			// StoppingCriterion<State> stoppingCriterion = new
			// StepLimitCriterion<>(numberOfSamplingSteps);
			DefaultSampler<State, List<Annotation>> sampler = new DefaultSampler<>(model, scorer, objective, explorers,
					objectiveOne);

			/*
			 * Define a learning strategy. The learner will receive state pairs
			 * which can be used to update the models parameters.
			 */
			DefaultLearner<State> learner = new DefaultLearner<>(model, 0.1);

			log.info("####################");
			log.info("Start training");

			/*
			 * The trainer will loop over the data and invoke sampling and
			 * learning. Additionally, it can invoke predictions on new data.
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
				state.getDocument().setAnnotations(new ArrayList<>(state.getEntities()));
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
			EvaluationUtil.printWeights(model, -1);

			avrgTrain = Evaluator.add(avrgTrain, trainEvaluation);

			/*
			 * Same for testdata
			 */

			List<State> testResults = trainer.test(sampler, initializer, test);
			for (State state : testResults) {
				state.getDocument().setAnnotations(new ArrayList<>(state.getEntities()));
			}
			Map<String, Double> testEvaluation = Evaluator.evaluateAll(test);
			log.info("Evaluation on test data:");
			testEvaluation.entrySet().forEach(e -> log.info(e));
			/*
			 * Finally, print the models weights.
			 */
			log.info("Model weights:");
			EvaluationUtil.printWeights(model, -1);
			avrgTest = Evaluator.add(avrgTest, testEvaluation);
		}
		/*
		 * Compute avrg. scores from sum of scores
		 */
		avrgTrain.entrySet().forEach(e -> e.setValue(e.getValue() / n));
		avrgTest.entrySet().forEach(e -> e.setValue(e.getValue() / n));

		log.info("%s-fold cross validation on TRAIN:", n);
		avrgTrain.entrySet().forEach(e -> log.info(e));

		log.info("%s-fold cross validation on TEST:", n);
		avrgTest.entrySet().forEach(e -> log.info(e));
	}
}
