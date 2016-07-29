package de.citec.sc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.FeatureUtils;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
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
import de.citec.sc.weka.WekaModelTrainer;
import exceptions.MissingFactorException;
import factors.Factor;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Vector;
import learning.scorer.LinearScorer;
import learning.scorer.Scorer;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.stoppingcriterion.StoppingCriterion;
import templates.AbstractTemplate;
import utility.Utils;

public class SVMTrain {

    private static final String PARAM_SETTING_IDENTIFIER = "-s";

    private static final String PARAM_SETTING_DOCUMENTSIZE = "-n";
    private static final String PARAM_SETTING_DATASET = "-d";

    private static final String PARAM_SETTING_BINS = "-z";

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final int MAX_CANDIDATES = 100;
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

        CorpusLoader loader = new CorpusLoader(false);

        DefaultCorpus corpus = loader.loadCorpus(CorpusName.valueOf(dataset));
        List<Document> documents = corpus.getDocuments();

        documents = documents.stream().filter(d -> d.getGoldStandard().size() <= 50).collect(Collectors.toList());
        int dSize = Integer.parseInt(PARAMETERS.get(PARAM_SETTING_DOCUMENTSIZE));
        if (dSize < documents.size()) {
            documents = documents.subList(0, dSize);
        }

        System.out.println("Document size: " + documents.size());

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

//        log.info("Train data:");
//        train.forEach(s -> log.info("%s", s));
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
         * Create the scorer object that computes a score from the features of a
         * factor and the weight vectors of the templates.
         */
        Scorer scorer = new LinearScorer();
        
        /*
         * Define a model and provide it with the necessary templates.
         */
        Model<Document, State> model = new Model<>(scorer, templates);
        model.setMultiThreaded(true);
        

        /*
         * Create an Initializer that is responsible for providing an initial
         * state for the sampling chain given a sentence.
         */

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

        Initializer<Document, State> trainInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);

        // StoppingCriterion<State> stoppingCriterion = new
        // StepLimitCriterion<>(numberOfSamplingSteps);
        DefaultSampler<Document, State, List<Annotation>> sampler = new DefaultSampler<>(model, objective,
                explorers, objectiveOneCriterion);
        sampler.setTrainingSamplingStrategy(SamplingStrategies.greedyObjectiveStrategy());
        sampler.setTrainingAcceptStrategy(AcceptStrategies.strictObjectiveAccept());

        for (int d = 0; d < documents.size(); d++) {
            Document document = documents.get(d);
            log.info("===========TEST============");
            log.info("Document: %s/%s", d + 1, documents.size());
            log.info("Content   : %s", document);
            log.info("Gold Result: %s", document.getGoldResult());
            log.info("===========================");
            List<State> chain = new ArrayList<>();
            State currentState = trainInitializer.getInitialState(document);
            int step = 0;
            do {
                List<State> nextStates = explorer.getNextStates(currentState);
                List<State> allStates = new ArrayList<>(nextStates);
                if (nextStates.size() > 0) {
                    allStates.add(currentState);

                    Stream<State> stream = Utils.getStream(allStates, true);
                    stream.forEach(s -> objective.score(s, s.getInstance().getGoldResult()));

                    State candidateState = nextStates.stream().max((s1, s2) -> -Double.compare(s1.getObjectiveScore(), s2.getObjectiveScore())).get();

                    currentState = candidateState.getObjectiveScore() > currentState.getObjectiveScore() ? candidateState : currentState;

                    chain.add(currentState);

                    model.applyToStates(Arrays.asList(currentState), currentState.getFactorGraph().getFactorPool(), currentState.getInstance());

                    writeFeatures(featuresFileWriter, currentState);

                    log.info("Sampled State:  %s", currentState);
                    step++;
                }
            } while (!objectiveOneCriterion.checkCondition(chain, step));

            State finalState = chain.get(chain.size() - 1);

            finalState.getFactorGraph().clear();
            finalState.getFactorGraph().getFactorPool().clear();

            log.info("++++++++++++++++");
            log.info("Gold Result:   %s", document.getGoldResult());
            log.info("Final State:  %s", finalState);
            log.info("++++++++++++++++");
            log.info("===========================");
        }
        featuresFileWriter.close();

        /*
         * Stop sampling if model score does not increase for 5 iterations.
         */
        WekaModelTrainer.train("src/main/resources/features_" + generateNameFromTemplates(templates) + ".csv");
    }

    public static void writeFeatures(BufferedWriter featuresFileWriter,
            State currentState) {
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
//            builder.append("\n"+currentState.toString()+"\n");
//            builder.append("Factors\n");
//            currentState.getFactorGraph().getFactors().forEach(f -> builder.append(f+"\n"));
//            builder.append("=========================================================================================\n\n");
            featuresFileWriter.write(builder.toString());
            featuresFileWriter.newLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (MissingFactorException ex) {
            ex.printStackTrace();
        }
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
        System.out.println("Initializing index mapping");
        IndexMapping.init(tsprIndexMappingFile);
        explorer = new AllScoresExplorer(index, MAX_CANDIDATES);
        initializeTempaltesFromSetting(setting);

    }

    private static void initializeTempaltesFromSetting(Setting setting) {

        try {

            for (Class<? extends AbstractTemplate> template : setting.getSetting()) {
                // if (template.equals(IndexRankTemplate.class)) {
                //
                // }

                if (template.equals(TopicSpecificPageRankTemplate.class
                )) {
                    log.info(
                            "Init TopicSpecificPageRankTemplate ...");
                    TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);

                }

                if (template.equals(DocumentSimilarityTemplate.class
                )) {
                    log.info(
                            "Init DocumentSimilarityTemplate ...");
                    DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile,
                            true);
                }

                if (template.equals(PageRankTemplate.class
                )) {
                }

                if (template.equals(EditDistanceTemplate.class
                )) {
                }

                if (template.equals(TermFrequencyTemplate.class
                )) {
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
            if (template.equals(PageRankTemplate.class
            )) {
                templates.add(
                        new PageRankTemplate(false));
                log.info(
                        "Add tempalte: " + template.getSimpleName());
            }

            if (template.equals(EditDistanceTemplate.class
            )) {
                if (PARAMETERS.containsKey(PARAM_SETTING_BINS)) {
                    if (PARAMETERS.get(PARAM_SETTING_BINS).equals("false")) {
                        templates.add(new EditDistanceTemplate(false));
                    } else {
                        templates.add(new EditDistanceTemplate(true));
                    }
                } else {
                    templates.add(new EditDistanceTemplate(true));
                }
                log.info(
                        "Add tempalte: " + template.getSimpleName());
            }

            if (template.equals(TopicSpecificPageRankTemplate.class
            )) {

                try {
                    if (PARAMETERS.containsKey(PARAM_SETTING_BINS)) {
                        if (PARAMETERS.get(PARAM_SETTING_BINS).equals("false")) {
                            templates.add(new TopicSpecificPageRankTemplate(false));
                        } else {
                            templates.add(new TopicSpecificPageRankTemplate(true));
                        }
                    } else {
                        templates.add(new TopicSpecificPageRankTemplate(true));
                    }
                } catch (InitializationException e) {
                    e.printStackTrace();
                    System.exit(0);
                }

                log.info(
                        "Add tempalte: " + template.getSimpleName());
            }

            if (template.equals(TermFrequencyTemplate.class
            )) {
                templates.add(
                        new TermFrequencyTemplate(false));
                log.info(
                        "Add tempalte: " + template.getSimpleName());
            }

            if (template.equals(DocumentSimilarityTemplate.class
            )) {

                try {
                    templates.add(new DocumentSimilarityTemplate());
                } catch (InitializationException e) {
                    e.printStackTrace();
                    System.exit(0);
                }

                log.info(
                        "Add tempalte: " + template.getSimpleName());
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
