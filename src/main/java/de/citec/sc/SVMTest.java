package de.citec.sc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import de.citec.sc.weka.WekaRegression;
import evaluation.EvaluationUtil;
import exceptions.UnkownTemplateRequestedException;
import java.util.HashMap;
import java.util.stream.Collectors;
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
import templates.AbstractTemplate;

public class SVMTest {

    private static Logger log = LogManager.getFormatterLogger();
    private static String indexFile = "tfidf.bin";
    private static String dfFile = "en_wiki_large_abstracts.docfrequency";
    private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
    private static String tsprFile = "tspr.gold";
    private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

    private static final int MAX_CANDIDATES = 100;
    private static final Map<String, String> PARAMETERS = new HashMap<>();
    private static final String PARAM_SETTING_BINS = "-z";

    private static final String PARAMETER_PREFIX = "-";

    private static Scorer scorer;
    private static Model model;

    public static void main(String[] args) throws UnkownTemplateRequestedException, Exception {

        System.out.println("Initializing templates");
        log.info("Init TopicSpecificPageRankTemplate ...");
        TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);
        IndexMapping.init(tsprIndexMappingFile);
        log.info("Init DocumentSimilarityTemplate ...");
        DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);

        /*
         * Only for local runs.
         */
        // args = new String[] { "src/main/resources/models/PR", "CoNLLTesta" };
        String modelDirPath = args[0];
        String modelName = args[0].replace("src/main/resources/", "").replace("features_", "").replace(".model", "");

        List<AbstractTemplate<Document, State, ?>> templates = new ArrayList<>();
        String[] t = modelName.split("-");

        for (int i = 0; i < t.length; i++) {
            if (t[i].equals("PR")) {
                templates.add(new PageRankTemplate());
            }
            if (t[i].equals("TF")) {
                templates.add(new TermFrequencyTemplate());
            }
            if (t[i].equals("DS")) {
                templates.add(new DocumentSimilarityTemplate());
            }
            if (t[i].equals("ED")) {
                if (PARAMETERS.containsKey(PARAM_SETTING_BINS)) {
                    if (PARAMETERS.get(PARAM_SETTING_BINS).equals("false")) {
                        templates.add(new EditDistanceTemplate(false));
                    } else {
                        templates.add(new EditDistanceTemplate(true));
                    }
                } else {
                    templates.add(new EditDistanceTemplate(true));
                }

            }
            if (t[i].equals("TSPR")) {
                if (PARAMETERS.containsKey(PARAM_SETTING_BINS)) {
                    if (PARAMETERS.get(PARAM_SETTING_BINS).equals("false")) {
                        templates.add(new TopicSpecificPageRankTemplate(false));
                    } else {
                        templates.add(new TopicSpecificPageRankTemplate(true));
                    }
                } else {
                    templates.add(new TopicSpecificPageRankTemplate(true));
                }
            }
        }

        // load svm scorer
        scorer = new WekaRegression("src/main/resources/features_" + modelName + ".model",
                "src/main/resources/features_" + modelName + ".arff");
        /*
         * Define a model and provide it with the necessary templates.
         */
        model = new Model(scorer, templates);
        model.setForceFactorComputation(false);
        model.setMultiThreaded(true);

        String corpusName = args[1];
        log.info("Load Corpus...");
        // CorpusLoader loader = new CorpusLoader();
        CorpusLoader loader = new CorpusLoader(false);
        DefaultCorpus corpus = null;
        switch (corpusName) {
            case "CoNLLTesta":
                corpus = loader.loadCorpus(CorpusName.CoNLLTesta);
                break;
            case "CoNLLTestb":
                corpus = loader.loadCorpus(CorpusName.CoNLLTestb);
                break;
            default:
                log.error("No dataset found for %s", corpusName);
                System.exit(1);
        }
        log.info("Test model in dir %s on dataset %s.", modelDirPath, corpusName);
        List<Document> documents = corpus.getDocuments();
        documents = documents.stream().filter(d -> d.getGoldStandard().size() <= 50).collect(Collectors.toList());

        testModel(modelDirPath, documents);
    }

    public static void testModel(String modelDirPath, List<Document> documents)
            throws UnkownTemplateRequestedException, Exception {

        File modelDir = new File(modelDirPath);

        List<Document> test = documents;

        log.info("Test data:");
        test.forEach(s -> log.info("%s", s));

        log.info("Load Index...");
        CandidateRetriever index = new CandidateRetrieverOnMemory();
		// CandidateRetriever index = new CandidateRetrieverOnLucene(false,
        // "mergedIndex");

        ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

        Initializer<Document, State> trainInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);

        List<Explorer<State>> explorers = new ArrayList<>();
        explorers.add(new AllScoresExplorer(index, MAX_CANDIDATES));
        int numberOfSamplingSteps = 200;

        StoppingCriterion<State> stopAtMaxModelScore = new StoppingCriterion<State>() {

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
        DefaultSampler<Document, State, List<Annotation>> sampler = new DefaultSampler<>(model, objective,
                explorers, stopAtMaxModelScore);

        sampler.setSamplingStrategy(SamplingStrategies.greedyModelStrategy());
        sampler.setAcceptStrategy(AcceptStrategies.strictModelAccept());

        log.info("####################");
        log.info("Start testing ...");

        Trainer trainer = new Trainer();

		// Initializer<Document, State> testInitializer = new
        // DisambiguationInitializer(index, false);
        // List<State> testResults = trainer.test(sampler, testInitializer,
        // test);
        List<State> testResults = trainer.test(sampler, trainInitializer, test);

        for (State state : testResults) {
            state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
        }

        Map<String, Double> testEvaluation = Evaluator.evaluateAll(test);
        log.info("Evaluation on test data:");
        testEvaluation.entrySet().forEach(e -> log.info(e));

        /*
         * Finally, print the models weights.
         */
		// log.info("Model weights:");
        // EvaluationUtil.printWeights(model, -1);
    }

    private static void readParamsFromCommandLine(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith(PARAMETER_PREFIX)) {
                    PARAMETERS.put(args[i], args[i++ + 1]); // Skip value
                }
            }
        }
    }

}
