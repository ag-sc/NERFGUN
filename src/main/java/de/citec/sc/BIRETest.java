package de.citec.sc;

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
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.templates.NEDTemplateFactory;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import de.citec.sc.variables.State;
import evaluation.EvaluationUtil;
import exceptions.UnkownTemplateRequestedException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Trainer;
import learning.scorer.LinearScorer;
import learning.scorer.Scorer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sampling.DefaultSampler;
import sampling.Explorer;
import sampling.Initializer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.stoppingcriterion.StoppingCriterion;

public class BIRETest {

    private static Logger log = LogManager.getFormatterLogger();
    private static String indexFile = "tfidf.bin";
    private static String dfFile = "en_wiki_large_abstracts.docfrequency";
    private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
    private static String tsprFile = "tspr.gold";
    private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

    private static int MAX_CANDIDATES = 100;
    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";

    private static final String PARAM_SETTING_BINS = "-z";

    public static void main(String[] args) throws UnkownTemplateRequestedException, Exception {

        readParamsFromCommandLine(args);
        /*
         * Only for local runs.
         */
        // args = new String[] { "src/main/resources/models/PR", "CoNLLTesta" };
        String modelDirPath = args[0];

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
            case "MicroTagging":
                corpus = loader.loadCorpus(CorpusName.MicroTagging);
                break;
            default:
                log.error("No dataset found for %s", corpusName);
                System.exit(1);
        }
        log.info("Test model in dir %s on dataset %s.", modelDirPath, corpusName);
        List<Document> documents = corpus.getDocuments();
        //documents = documents.stream().filter(d -> d.getGoldStandard().size() > 70).collect(Collectors.toList());

        testModel(modelDirPath, documents);
    }

    public static void testModel(String modelDirPath, List<Document> documents)
            throws UnkownTemplateRequestedException, Exception {

        File modelDir = new File(modelDirPath);

        List<Document> testUnfiltered = documents;

        Map<Document, Document> mapOfDocs = new LinkedHashMap<Document, Document>();

        List<Document> test = new ArrayList<>();
        
        int capacity = 70;

        /**
         * divides the documents which have higher number of gold standard annotations than 70 into bins with 50 annotatiosn
         */
        for (int i1 = 0; i1 < testUnfiltered.size(); i1++) {

            if (testUnfiltered.get(i1).getGoldStandard().size() > capacity) {

                List<Annotation> resizedGoldStandard = new ArrayList<>();

                for (int j = 0; j < testUnfiltered.get(i1).getGoldStandard().size(); j++) {

                    if (resizedGoldStandard.size() == 50) {
                        Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                        d1.setGoldStandard(resizedGoldStandard);
                        resizedGoldStandard = new ArrayList<>();

                        test.add(d1);

                        mapOfDocs.put(d1, testUnfiltered.get(i1));
                        resizedGoldStandard.add(testUnfiltered.get(i1).getGoldStandard().get(j));

                    } else {
                        resizedGoldStandard.add(testUnfiltered.get(i1).getGoldStandard().get(j));
                    }
                }

                //add the remaining
                if (resizedGoldStandard.size() <= 20) {
                    test.get(test.size() - 1).getGoldStandard().addAll(resizedGoldStandard);
                } else {
                    //create a new document
                    Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                    d1.setGoldStandard(resizedGoldStandard);
                    resizedGoldStandard = new ArrayList<>();

                    test.add(d1);

                    mapOfDocs.put(d1, testUnfiltered.get(i1));
                }
            } else {
                Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                d1.setGoldStandard(testUnfiltered.get(i1).getGoldStandard());

                test.add(d1);
                mapOfDocs.put(d1, testUnfiltered.get(i1));
            }
        }

        log.info("Document size : " + test.size());
        log.info("Load Index...");
        CandidateRetriever index = new CandidateRetrieverOnMemory();
        // CandidateRetriever index = new CandidateRetrieverOnLucene(false,
        // "mergedIndex");

        ObjectiveFunction<State, List<Annotation>> objective = new DisambiguationObjectiveFunction();

        log.info("Init TopicSpecificPageRankTemplate ...");
        TopicSpecificPageRankTemplate.init(tsprIndexMappingFile, tsprFile);
        IndexMapping.init(tsprIndexMappingFile);
        log.info("Init DocumentSimilarityTemplate ...");
        DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);

        boolean useBins = true;
        if (PARAMETERS.containsKey(PARAM_SETTING_BINS)) {
            if (PARAMETERS.get(PARAM_SETTING_BINS).equals("false")) {
                useBins = false;
            }
        }

        NEDTemplateFactory factory = new NEDTemplateFactory(useBins);

        Scorer scorer = new LinearScorer();

        /*
         * Define a model and provide it with the necessary templates.
         */
        Model<Document, State> model = new Model<>(scorer);
        model.setForceFactorComputation(false);
        //model.setSequentialScoring(true);
        model.setMultiThreaded(true);
        model.loadModelFromDir(modelDir, factory);

        // Scorer scorer = new LinearScorer();
        // }
        // if (PARAMETERS.get(PARAM_SCORER).equals("linear")) {
        // scorer = new LinearScorer();
        // }
        Initializer<Document, State> testInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);

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

        List<State> testResults = trainer.test(sampler, testInitializer, test);

        for (State state : testResults) {
            Document original = mapOfDocs.get(state.getInstance());

            List<Annotation> a = original.getAnnotations();
            a.addAll(new ArrayList<>(state.getEntities()));
            original.setAnnotations(a);

//            List<Annotation> g = original.getGoldStandard();
//            g.addAll(state.getInstance().getGoldStandard());
//            original.setGoldStandard(g);
            //state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
        }

        Map<String, Double> testEvaluation = Evaluator.evaluateAll(testUnfiltered);
        log.info("Evaluation on test data:");
        testEvaluation.entrySet().forEach(e -> log.info(e));

        /*
         * Finally, print the models weights.
         */
        log.info("Model weights:");
        EvaluationUtil.printWeights(model, -1);

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
