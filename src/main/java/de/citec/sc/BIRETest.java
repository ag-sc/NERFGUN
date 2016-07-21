package de.citec.sc;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.CorpusLoader.CorpusName;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.helper.DBpediaEndpoint;
import de.citec.sc.helper.DocumentUtils;
import de.citec.sc.helper.SortUtils;
import de.citec.sc.learning.DisambiguationObjectiveFunction;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.CandidateRetrieverOnMemory;
import de.citec.sc.sampling.AllScoresExplorer;
import de.citec.sc.sampling.AlternativeInitializer;
import de.citec.sc.sampling.DisambiguationInitializer;
import de.citec.sc.templates.CandidateSimilarityTemplate;
import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.templates.NEDTemplateFactory;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import de.citec.sc.variables.State;
import edu.stanford.nlp.io.PrintFile;
import evaluation.EvaluationUtil;
import exceptions.UnkownTemplateRequestedException;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
//    private static String dfFile = "en_wiki_large_abstracts.docfrequency";
    private static String dfFile = "en_wiki.docfrequency";
//    private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
    private static String tfidfFile = "en_wiki.tfidf";
    private static String tsprFile = "tspr.all";
    private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";

    private static int MAX_CANDIDATES = 1;
    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";

    private static final String PARAM_SETTING_BINS = "-z";

    private static final String PARAM_SETTING_MAX_CANDIDATES = "-n";

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
            case "MicroTag2014Train":
                corpus = loader.loadCorpus(CorpusName.MicroTag2014Train);
                break;
            case "MicroTag2014Test":
                corpus = loader.loadCorpus(CorpusName.MicroTag2014Test);
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

        Map<Integer, Integer> mapOfDocs = new LinkedHashMap<>();

        List<Document> test = new ArrayList<>();

        int capacity = 70;

        /**
         * divides the documents which have higher number of gold standard
         * annotations than 70 into bins with 50 annotatiosn
         */
        for (int i1 = 0; i1 < testUnfiltered.size(); i1++) {

            if (testUnfiltered.get(i1).getGoldStandard().size() > capacity) {

                List<Annotation> resizedGoldStandard = new ArrayList<>();

                for (int j = 0; j < testUnfiltered.get(i1).getGoldStandard().size(); j++) {

                    if (resizedGoldStandard.size() == 50) {
                        Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                        for (Annotation a : resizedGoldStandard) {
                            d1.getGoldStandard().add(a.clone());
                        }

                        resizedGoldStandard.clear();

                        test.add(d1);

                        mapOfDocs.put(test.size() - 1, i1);
                        resizedGoldStandard.add(testUnfiltered.get(i1).getGoldStandard().get(j));

                    } else {
                        resizedGoldStandard.add(testUnfiltered.get(i1).getGoldStandard().get(j));
                    }
                }

                //add the remaining
                if (resizedGoldStandard.size() <= 20) {
                    for (Annotation a : resizedGoldStandard) {
                        test.get(test.size() - 1).getGoldStandard().add(a.clone());
                    }
                    resizedGoldStandard.clear();
                } else {
                    //create a new document
                    Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                    for (Annotation a : resizedGoldStandard) {
                        d1.getGoldStandard().add(a.clone());
                    }

                    resizedGoldStandard.clear();

                    test.add(d1);

                    mapOfDocs.put(test.size() - 1, i1);
                }
            } else {
                Document d1 = new Document(testUnfiltered.get(i1).getDocumentContent(), testUnfiltered.get(i1).getDocumentName());
                for (Annotation a : testUnfiltered.get(i1).getGoldStandard()) {
                    d1.getGoldStandard().add(a.clone());
                }

                test.add(d1);
                mapOfDocs.put(test.size() - 1, i1);
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
//        DocumentSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
        CandidateSimilarityTemplate.init(indexFile, tfidfFile, dfFile, true);
        log.info("Init DBpedia data loading into memory ...");
        DBpediaEndpoint.init();

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
        List<Explorer<State>> explorers = new ArrayList<>();

        if (PARAMETERS.containsKey(PARAM_SETTING_MAX_CANDIDATES)) {
            MAX_CANDIDATES = Integer.parseInt(PARAMETERS.get(PARAM_SETTING_MAX_CANDIDATES));
        }

        Initializer<Document, State> testInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);
//        Initializer<Document, State> testInitializer = new AlternativeInitializer(index, MAX_CANDIDATES, true);

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
                final int maxCount = 2;

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

        long startTime = System.currentTimeMillis();
        List<State> testResults = trainer.test(sampler, testInitializer, test);
        long endTime = System.currentTimeMillis();

        //get total number of annotations
        int annotationSize = 0;
        //assign annotations to original dataset
        for (State state : testResults) {
            int indexOfSplitDoc = testResults.indexOf(state);
            int indexOfOrigDoc = mapOfDocs.get(indexOfSplitDoc);
            Document original = testUnfiltered.get(indexOfOrigDoc);

            if (original == null) {

                System.out.println("PROBLEM WITH EVALUTATION\n\n" + state.toString());

                System.exit(0);
            }

            List<Annotation> a = original.getAnnotations();
            a.addAll(new ArrayList<>(state.getEntities()));
            original.setAnnotations(a);

            annotationSize += a.size();

//            List<Annotation> g = original.getGoldStandard();
//            g.addAll(state.getInstance().getGoldStandard());
//            original.setGoldStandard(g);
            //state.getInstance().setAnnotations(new ArrayList<>(state.getEntities()));
        }

        log.info("Replacing Person names ...");
//        //postprocess assigned URIs to replace surnames with fullname URIs
        for (Document d : testUnfiltered) {

            //modify some annotations
            for (Annotation a1 : d.getAnnotations()) {
                for (Annotation a2 : d.getAnnotations()) {

                    if (!a1.equals(a2)) {
                        if (a1.getWord().toLowerCase().contains(a2.getWord().toLowerCase()) && a1.getWord().length() > a2.getWord().length() && a1.getEndIndex()< a2.getEndIndex()) {
                            Set<String> classes = DBpediaEndpoint.getClasses(a1.getLink());
                            if (classes.contains("Person")) {
                                System.out.println("Found here: a1" + a1);
                                System.out.println("Found here: a2" + a2);
                                for (Annotation g : d.getGoldStandard()) {
                                    if (g.getWord().equals(a1.getWord()) && g.getStartIndex() == a1.getStartIndex() && g.getEndIndex() == a1.getEndIndex()) {
                                        System.out.println("GoldStandard here a1: " + g);
                                    }
                                    if (g.getWord().equals(a2.getWord()) && g.getStartIndex() == a2.getStartIndex() && g.getEndIndex() == a2.getEndIndex()) {
                                        System.out.println("GoldStandard here a2: " + g);
                                    }
                                }
                                a2.setLink(a1.getLink());
                                System.out.println("Edited here: " + a1 + "    " + a2);
                                break;
                                
                            }
                        }
                    }
                }
            }
        }
        
        log.info("... Done!\n");
        for (Document d : testUnfiltered) {

            
            for (Annotation g : d.getGoldResult()) {
                boolean isGoldThere = false;
                for(Annotation a : d.getAnnotations()){
                    if(g.getEndIndex() == a.getEndIndex() && g.getStartIndex() == a.getStartIndex() && g.getWord().equals(a.getWord())){
                        isGoldThere = true;
                        break;
                    }
                }
                
                if(!isGoldThere){
                    System.out.println("Gold: "+g);
                }
                
                if(!isGoldThere){
                    String link = "";
                    for(Annotation a : d.getAnnotations()){
                        if(g.getWord().equals(a.getWord())){
                            link = a.getLink();
                        }
                    }
                    
                    Annotation a = new Annotation(g.getWord(), link, g.getStartIndex(), g.getEndIndex());
                    d.addAnnotation(a);
                }
            }
        }
        
//        for (Document d : testUnfiltered.subList(testUnfiltered.size()-1, testUnfiltered.size())) {
//            System.out.println(d+"\n\n");
//        }

        Map<String, Double> testEvaluation = Evaluator.evaluateAll(testUnfiltered);
        log.info("Evaluation on test data:");
        testEvaluation.entrySet().forEach(e -> log.info(e));

        System.out.println("Evaluation on test data:");
        testEvaluation.entrySet().forEach(e -> System.out.println(e));

        /*
         * Finally, print the models weights.
         */
        long runTimePerDoc = (endTime - startTime) / (long) test.size();
        long runTimePerAnt = (endTime - startTime) / (long) annotationSize;
        log.info("\nRuntime per doc: " + runTimePerDoc + " ms\n");
        log.info("Runtime per annotation: " + runTimePerAnt + " ms\n");
        System.out.println("\nRuntime per doc: " + runTimePerDoc + " ms\n");
        System.out.println("Runtime per annotation: " + runTimePerAnt + " ms\n");

        //get results and write to the file
        //get current model name and Micro F1
        Date d = new Date();
        String nameOfModel = "Setting : " + modelDirPath + " Date : " + d.toString();
        double performanceOfModel = 0;
//        results += "\nEvaluation on test data:";
        for (String r1 : testEvaluation.keySet()) {
            if (r1.equals("F1 Micro-average")) {
//                results += testEvaluation.get(r1) + "\n";
                performanceOfModel = testEvaluation.get(r1);
            }
        }

        String resultPath = "src/main/resources/results.txt";
        //read previous results
        Set<String> previousModels = DocumentUtils.readFile(new File(resultPath));
        HashMap<String, Double> resultsFromFile = new HashMap<>();

        //add them to map
        if (previousModels != null) {
            for (String p1 : previousModels) {
                String modelName = p1.substring(0, p1.indexOf("=")).trim();
                String value = p1.substring(p1.indexOf("=") + 1).trim();
                resultsFromFile.put(modelName, Double.parseDouble(value));
            }
        }

        resultsFromFile.put(nameOfModel, performanceOfModel);

        resultsFromFile = SortUtils.sortByDoubleValue(resultsFromFile);

        String results = "";
        for (String modelName : resultsFromFile.keySet()) {
            results += modelName + "=" + resultsFromFile.get(modelName) + "\n";
        }

        DocumentUtils.writeListToFile(resultPath, results.trim(), false);

        log.info("Model weights:");
        EvaluationUtil.printWeights(model, -1);

        log.info("Print wrong annotations.....");
//        printErrors(testUnfiltered);
        log.info("Done!");
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

    private static void printErrors(List<Document> annotatedDocs) {

        List<String> errors = new ArrayList<>();
        HashMap<String, Integer> frequentTypes = new HashMap<>();

        for (Document d : annotatedDocs) {
            String error = "Text:\n" + d.getDocumentContent();

            error += "\n\nGold Annotations:";
            for (Annotation gold : d.getGoldResult()) {
                error += "\n" + gold;
            }

            boolean hasError = false;
            error += "\n\nWrong Annotations:";

            for (Annotation a : d.getAnnotations()) {

                if (!d.getGoldStandard().contains(a)) {
                    hasError = true;

                    String e = "Found URI: " + a.getLink();
                    for (Annotation gold : d.getGoldResult()) {
                        if (gold.getWord().equals(a.getWord()) && gold.getStartIndex() == a.getStartIndex() && gold.getEndIndex() == a.getEndIndex()) {
                            e += "    Expected URI: " + gold.getLink();

                            //add the type of the expected type
                            Set<String> typesForURI = DBpediaEndpoint.getClasses(gold.getLink());

                            if (typesForURI != null) {
                                for (String t : typesForURI) {
                                    frequentTypes.put(t, frequentTypes.getOrDefault(t, 1) + 1);
                                }
                            }

                            break;
                        }
                    }

                    e += "   Label: " + a.getWord() + "    StartIndex: " + a.getStartIndex() + "   EndIndex: " + a.getEndIndex();
                    error += "\n" + e;
                }
            }

            if (hasError) {
                errors.add(error);
            }
        }

        //split into 5 docs
        int split = errors.size() / 5;

        for (int i = 1; i <= 5; i++) {

            int start = ((i - 1) * split);
            int end = ((i) * split);
            List<String> partial = errors.subList(start, end);

            if (errors.size() % 5 != 0 && i == 1) {

                List<String> remaining = errors.subList(split * 5, errors.size());
                partial.addAll(remaining);
            }

            if (!partial.isEmpty()) {
                String r = "";

                for (String s : partial) {
                    r += "\n" + s;
                    r += "\n==========================================================================================================================\n";
                }

                DocumentUtils.writeListToFile("errors" + i + ".txt", r, false);
            }

        }

        //write out types
        frequentTypes = SortUtils.sortByValue(frequentTypes);

        String result = "";

        for (String t : frequentTypes.keySet()) {
            result += t + "    " + frequentTypes.get(t) + "\n";
        }

        DocumentUtils.writeListToFile("wrongAnnotationTypes.txt", result.trim(), false);
    }

}
