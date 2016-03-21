package de.citec.sc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.Instance;
import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
import de.citec.sc.learning.FeatureUtils;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.sampling.AllScoresExplorer;
import de.citec.sc.sampling.DisambiguationInitializer;
import de.citec.sc.settings.BIRESettings;
import de.citec.sc.settings.Setting;
import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.templates.InitializationException;
import de.citec.sc.templates.PageRankTemplate;
import de.citec.sc.templates.TermFrequencyTemplate;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import de.citec.sc.variables.State;
import evaluation.EvaluationUtil;
import exceptions.MissingFactorException;
import factors.Factor;
import java.util.Set;
import learning.DefaultLearner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Trainer;
import learning.Vector;
import learning.callbacks.StepCallback;
import learning.scorer.LinearScorer;
import learning.scorer.Scorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.stoppingcriterion.StoppingCriterion;
import templates.AbstractTemplate;
import variables.AbstractState;

public class BIREMain {

    private static final String PARAM_SETTING_IDENTIFIER = "-s";

    private static final String PARAM_SETTING_DOCUMENTSIZE = "-n";
    private static final String PARAM_SETTING_DATASET = "-d";

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";

    private static Logger log = LogManager.getFormatterLogger();

    private static String indexFile = "tfidf.bin";
    private static String dfFile = "en_wiki_large_abstracts.docfrequency";
    private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
    private static String tsprFile = "tspr.gold";
    private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";
    private static CandidateRetriever index;
    private static Setting setting;

    private static Explorer<State> explorer;

    /**
     * Read the parameters from the command line.
     *
     * @param args
     */
    private static void readParamsFromCommandLine(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith(PARAMETER_PREFIX)) {
                    PARAMETERS.put(args[i], args[i++ + 1]); // Skip value
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {

        /*
         * TODO: Just for testing !!!! Remove before Jar export.
         */
//        args = new String[]{"-s", "7"};
        // LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        // Configuration config = ctx.getConfiguration();
        // LoggerConfig loggerConfig =
        // config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        // loggerConfig.setLevel(Level.INFO);
        // ctx.updateLoggers();
        initializeBIRE(args);

        /*
         * Define templates that are responsible to generate factors/features to
         * score intermediate, generated states.
         */
        List<AbstractTemplate<Document, State, ?>> templates = new ArrayList<>();

        addTemplatesFromSetting(templates);

        File modelsDir = new File("src/main/resources/models");
        File featuresFile = new File("src/main/resources/features_" + generateNameFromTemplates(templates) + ".csv");
        BufferedWriter featuresFileWriter = new BufferedWriter(new FileWriter(featuresFile));

        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }

        /*
         * Load training and test data.
         */
        log.info("Load Corpus...");

        String dataset = PARAMETERS.get(PARAM_SETTING_DATASET);

        CorpusLoader loader = new CorpusLoader();

        DefaultCorpus corpus = loader.loadCorpus(CorpusName.valueOf(dataset));
        List<Document> documents = corpus.getDocuments();

        documents = documents.stream().filter(d -> d.getGoldStandard().size() <= 50).collect(Collectors.toList());
        int dSize = Integer.parseInt(PARAMETERS.get(PARAM_SETTING_DOCUMENTSIZE));
        if (dSize < documents.size()) {
            documents = documents.subList(0, dSize);
        }

        int numberOfEpochs = 1;

        /*
         * Some code for n-fold cross validation
         */
        Map<String, Double> avrgTrain = new LinkedHashMap<>();
        Map<String, Double> avrgTest = new LinkedHashMap<>();
        // Collections.shuffle(documents);
        Collections.shuffle(documents, new Random(100l));

        // int N = documents.size();
        // int n = 2;
        // double step = ((float) N) / n;
        // double k = 0;
        // for (int i = 0; i < n; i++) {
        // System.out.println("Train");
        // log.info("Cross-Validation Fold %s/%s", i + 1, n);
        // double j = k;
        // k = j + step;
        // List<Document> test = documents;(IOException e1) {
        // e1.printStackTrace();
        // System.exit(1);
        // }
        List<Document> train = documents;
        // List<Document> test = documents.subList((int) Math.floor(j), (int)
        // Math.floor(k));
        // List<Document> train = new ArrayList<>(documents);
        // train.removeAll(test);

        log.info("Train data:");
        train.forEach(s -> log.info("%s", s));

        // log.info("Test data:");
        // test.forEach(s -> log.info("%s", s));
		/*
         * In the following, we setup all necessary components for training and
         * testing.
         */
        /*
         * Define an objective function that guides the training procedure.
         */
        ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

        // templates.add(new PageRankTemplate());

        /*
         * Define a model and provide it with the necessary templates.
         */
        Model<Document, State> model = new Model<>(templates);
        model.setMultiThreaded(true);
        /*
         * Create the scorer object that computes a score from the features of a
         * factor and the weight vectors of the templates.
         */
        Scorer scorer = new LinearScorer();

        /*
         * Create an Initializer that is responsible for providing an initial
         * state for the sampling chain given a sentence.
         */
        Initializer<Document, State> trainInitializer = new DisambiguationInitializer(index, true);

        /*
         * Define the explorers that will provide "neighboring" states given a
         * starting state. The sampler will select one of these states as a
         * successor state and, thus, perform the sampling procedure.
         */
        List<Explorer<State>> explorers = new ArrayList<>();
        // explorers.add(new DisambiguationExplorer(index));
        explorers.add(explorer);
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
                if (chain.isEmpty()) {
                    return false;
                }

                double maxScore = chain.get(chain.size() - 1).getObjectiveScore();
                if (maxScore >= 1) {
                    return true;
                }
                int count = 0;
                final int maxCount = 5;

                for (int i = 0; i < chain.size(); i++) {
                    if (chain.get(i).getObjectiveScore() >= maxScore) {
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
        sampler.addStepCallback(new StepCallback() {

            public <InstanceT extends Instance, StateT extends AbstractState<InstanceT>> void onEndStep(
                    DefaultSampler<InstanceT, StateT, ?> defaultSampler, int step, int e, int size, StateT initialState,
                    StateT currentState) {
                StringBuilder builder = new StringBuilder();
                try {
                    Vector features = FeatureUtils.mergeFeatures((Set<Factor<?>>) currentState.getFactorGraph().getFactors());
                    builder.append(currentState.getObjectiveScore());
                    builder.append("\t");
                    for (Entry<String, Double> feature : features.getFeatures().entrySet()) {
                        builder.append(feature.getKey());
                        builder.append("\t");
                        builder.append(feature.getValue());
                        builder.append("\t");
                    }
                    featuresFileWriter.write(builder.toString());
                    featuresFileWriter.newLine();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (MissingFactorException ex) {
                    ex.printStackTrace();
                }
            }
        });
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
        Trainer trainer = new Trainer();

        trainer.train(sampler, trainInitializer, learner, train, numberOfEpochs);
        featuresFileWriter.close();
        /*
         * Stop sampling if model score does not increase for 5 iterations.
         */
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

        sampler.setStoppingCriterion(stopAtMaxModelScore);

        // Initializer<Document, State> testInitializer = new
        // DisambiguationInitializer(index, false);
        // List<State> testResults = trainer.test(sampler, testInitializer,
        // test);
        // List<State> testResults = trainer.test(sampler, trainInitializer,
        // test);
        // for (State state : testResults) {
        // state.getInstance().setAnnotations(new
        // ArrayList<>(state.getEntities()));
        // }
        // Map<String, Double> testEvaluation = Evaluator.evaluateAll(test);
        // log.info("Evaluation on test data:");
        // testEvaluation.entrySet().forEach(e -> log.info(e));

        /*
         * Finally, print the models weights.
         */
        log.info("Model weights:");
        EvaluationUtil.printWeights(model, -1);

        System.out.println("Write model:");
        model.saveModelToFile(modelsDir, generateNameFromTemplates(templates));
		// avrgTest = Evaluator.add(avrgTest, testEvaluation);

		// /*
        // * Perform prediction on training and test data.
        // */
        // List<State> trainResults = trainer.test(sampler, initializer,
        // train);
        //
        // /*
        // * Give the final annotations to the Document for the Evaluator
        // */
        // for (State state : trainResults) {
        // state.getInstance().setAnnotations(new
        // ArrayList<>(state.getEntities()));
        // }
        // /*
        // * Evaluate train and test predictions
        // */
        // Map<String, Double> trainEvaluation =
        // Evaluator.evaluateAll(train);
        //
        // /*
        // * Print evaluation
        // */
        // log.info("Evaluation on training data:");
        // trainEvaluation.entrySet().forEach(e -> log.info(e));
        //
        // /*
        // * Finally, print the models weights.
        // */
        // log.info("Model weights:");
        // EvaluationUtil.printWeights(model, 0);
        //
        // avrgTrain = Evaluator.add(avrgTrain, trainEvaluation);
        //
        // /*
        // * Same for testdata
        // */
        // }
		/*
         * Compute avrg. scores from sum of scores
         */
        // avrgTest.entrySet().forEach(e -> e.setValue(e.getValue() / n));
        // log.info("%s-fold cross validation on TEST:", n);
        // avrgTest.entrySet().forEach(e -> log.info(e));
        // avrgTrain.entrySet().forEach(e -> e.setValue(e.getValue() / n));
        // log.info("%s-fold cross validation on TRAIN:", n);
        // avrgTrain.entrySet().forEach(e -> log.info(e));
    }

    private static String generateNameFromTemplates(Collection<?> templates) {
        String name = "";
        String dash = "";
        for (Object template : templates) {
            name += dash;
            if (template instanceof TopicSpecificPageRankTemplate) {
                name += "TSPR";
            } else if (template instanceof DocumentSimilarityTemplate) {
                name += "DS";
            } else if (template instanceof PageRankTemplate) {
                name += "PR";
            } else if (template instanceof EditDistanceTemplate) {
                name += "ED";
            } else if (template instanceof TermFrequencyTemplate) {
                name += "TF";
            }
            dash = "-";
        }
        return name;
    }

    private static void initializeBIRE(String[] args) {

        readParamsFromCommandLine(args);

        setting = BIRESettings.getSetting(Integer.parseInt(PARAMETERS.get(PARAM_SETTING_IDENTIFIER)));

        log.info("Template setting: " + setting.toString());

        /*
         * Load the index API.
         */
        log.info("Load Index...");
//        index = new CandidateRetrieverOnLucene(false, "mergedIndex");
         index = new CandidateRetrieverOnMemory();
        IndexMapping.init(tsprIndexMappingFile);
        explorer = new AllScoresExplorer(index);
        initializeTempaltesFromSetting(setting);

    }

    private static void initializeTempaltesFromSetting(Setting setting) {

        try {

            for (Class<? extends AbstractTemplate> template : setting.getSetting()) {
                // if (template.equals(IndexRankTemplate.class)) {
                //
                // }

                if (template.equals(TopicSpecificPageRankTemplate.class)) {
                    log.info("Init TopicSpecificPageRankTemplate ...");
                    TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);

                }
                if (template.equals(DocumentSimilarityTemplate.class)) {
                    log.info("Init DocumentSimilarityTemplate ...");
                    DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
                }

                if (template.equals(PageRankTemplate.class)) {
                }

                if (template.equals(EditDistanceTemplate.class)) {
                }

                if (template.equals(TermFrequencyTemplate.class)) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     *
     * @param setting
     * @param index
     * @param templates
     */
    private static void addTemplatesFromSetting(List<AbstractTemplate<Document, State, ?>> templates) {
        for (Class<? extends AbstractTemplate> template : setting.getSetting()) {

            // if (template.equals(IndexRankTemplate.class)) {
            // templates.add(new IndexRankTemplate());
            // log.info("Add tempalte: " + template.getSimpleName());
            // }
            if (template.equals(PageRankTemplate.class)) {
                templates.add(new PageRankTemplate());
                log.info("Add tempalte: " + template.getSimpleName());
            }
            if (template.equals(EditDistanceTemplate.class)) {
                templates.add(new EditDistanceTemplate());
                log.info("Add tempalte: " + template.getSimpleName());
            }
            if (template.equals(TopicSpecificPageRankTemplate.class)) {
                try {
                    templates.add(new TopicSpecificPageRankTemplate());
                } catch (InitializationException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                log.info("Add tempalte: " + template.getSimpleName());
            }
            if (template.equals(TermFrequencyTemplate.class)) {
                templates.add(new TermFrequencyTemplate());
                log.info("Add tempalte: " + template.getSimpleName());
            }
            if (template.equals(DocumentSimilarityTemplate.class)) {
                try {
                    templates.add(new DocumentSimilarityTemplate());
                } catch (InitializationException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
                log.info("Add tempalte: " + template.getSimpleName());
            }
        }
    }

}

// 15:41:50.191 [main] INFO - Micro-average Precision=0.524
// 15:41:50.191 [main] INFO - Micro-average Recall=0.519
// 15:41:50.191 [main] INFO - F1 Micro-average=0.5215
// 15:41:50.191 [main] INFO - Macro-average Precision=0.5365
// 15:41:50.191 [main] INFO - Macro-average Recall=0.5325
// 15:41:50.191 [main] INFO - F1 Macro-average=0.5345
// 15:38:00.653 [main] INFO - 2-fold cross validation on TEST:
// 15:38:00.654 [main] INFO - Micro-average Precision=0.5700000000000001
// 15:38:00.654 [main] INFO - Micro-average Recall=0.5645
// 15:38:00.654 [main] INFO - F1 Micro-average=0.5675
// 15:38:00.654 [main] INFO - Macro-average Precision=0.5945
// 15:38:00.654 [main] INFO - Macro-average Recall=0.589
// 15:38:00.654 [main] INFO - F1 Macro-average=0.5914999999999999
// 15:35:47.926 [main] INFO - 2-fold cross validation on TEST:
// 15:35:47.926 [main] INFO - Micro-average Precision=0.5235000000000001
// 15:35:47.927 [main] INFO - Micro-average Recall=0.5195000000000001
// 15:35:47.927 [main] INFO - F1 Micro-average=0.5215000000000001
// 15:35:47.927 [main] INFO - Macro-average Precision=0.537
// 15:35:47.927 [main] INFO - Macro-average Recall=0.534
// 15:35:47.927 [main] INFO - F1 Macro-average=0.5355000000000001
//
// 10 Epochs 10 docs
// 13:49:07.862 [main] INFO - 2-fold cross validation on TEST:
// 13:49:07.862 [main] INFO - Micro-average Precision=0.3675
// 13:49:07.862 [main] INFO - Micro-average Recall=0.359
// 13:49:07.862 [main] INFO - F1 Micro-average=0.363
// 13:49:07.862 [main] INFO - Macro-average Precision=0.45099999999999996
// 13:49:07.862 [main] INFO - Macro-average Recall=0.44899999999999995
// 13:49:07.862 [main] INFO - F1 Macro-average=0.44999999999999996
