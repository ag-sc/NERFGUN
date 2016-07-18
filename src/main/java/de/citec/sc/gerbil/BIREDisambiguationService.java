package de.citec.sc.gerbil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.helper.DBpediaEndpoint;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
import spark.Spark;
import templates.AbstractTemplate;
import templates.TemplateFactory;

public class BIREDisambiguationService implements TemplateFactory<Document, State> {

    private static Logger log = LogManager.getFormatterLogger();

    private static String indexFile = "tfidf.bin";
    private static String dfFile = "en_wiki_large_abstracts.docfrequency";
    private static String tfidfFile = "en_wiki_large_abstracts.tfidf";
    private static String tsprFile = "tspr.all";
    private static String tsprIndexMappingFile = "wikipagegraphdataDecoded.keys";
    private static int MAX_CANDIDATES = 10;
    private int numberOfSamplingSteps = 200;
    private boolean useBins = true;
    private int capacity = 70;

    public static final String DBPEDIA_LINK_PREFIX = "http://dbpedia.org/resource/";

//    private File modelDir;
    private CandidateRetriever index;
    private ObjectiveFunction<State, List<Annotation>> objective;
    private Scorer scorer;
    private Model<Document, State> model;
    private Model<Document, State> modelTwitter;
    private Initializer<Document, State> testInitializer;
    private List<Explorer<State>> explorers;
    private StoppingCriterion<State> stopAtMaxModelScore;
    private DefaultSampler<Document, State, List<Annotation>> sampler;
    private DefaultSampler<Document, State, List<Annotation>> samplerTwitter;
    private Trainer trainer;

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";
    private static final String PARAM_SETTING_MODEL = "-w";
    private static final String PARAM_SETTING_MODEL_TWITTER = "-n";

    private static void readParamsFromCommandLine(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith(PARAMETER_PREFIX)) {
                    PARAMETERS.put(args[i], args[i++ + 1]); // Skip value
                }
            }
        }
    }

    public static void main(String[] args)
            throws FileNotFoundException, IOException, UnkownTemplateRequestedException, Exception {

        readParamsFromCommandLine(args);

        BIREDisambiguationService service = new BIREDisambiguationService();
        String modelDirPath = PARAMETERS.get(PARAM_SETTING_MODEL);
        String modelDirPathTwitter = PARAMETERS.get(PARAM_SETTING_MODEL_TWITTER);
        service.init(modelDirPath, modelDirPathTwitter);
        service.run();
    }

    public BIREDisambiguationService() {
    }

    public void init(String modelDirPath, String modelDirPathTwitter)
            throws FileNotFoundException, IOException, UnkownTemplateRequestedException, Exception {
        log.info("Initialize service from model dir \"%s\" ...", modelDirPath);
        log.info("Initialize service from model dir \"%s\" ...", modelDirPathTwitter);
        index = new CandidateRetrieverOnMemory();

        objective = new DisambiguationObjectiveFunction();
        scorer = new LinearScorer();

        model = new Model<>(scorer);
        model.setForceFactorComputation(false);
        model.setMultiThreaded(true);
        model.loadModelFromDir(new File(modelDirPath), this);

        modelTwitter = new Model<>(scorer);
        modelTwitter.setForceFactorComputation(false);
        modelTwitter.setMultiThreaded(true);
        modelTwitter.loadModelFromDir(new File(modelDirPathTwitter), this);

        testInitializer = new DisambiguationInitializer(index, MAX_CANDIDATES, true);

        explorers = new ArrayList<>();
        IndexMapping.init(tsprIndexMappingFile);
        explorers.add(new AllScoresExplorer(index, MAX_CANDIDATES));
        
        DBpediaEndpoint.init();
        log.info("Initialization done! Ready for service");

        stopAtMaxModelScore = new StoppingCriterion<State>() {

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
        sampler = new DefaultSampler<>(model, objective, explorers, stopAtMaxModelScore);
        sampler.setSamplingStrategy(SamplingStrategies.greedyModelStrategy());
        sampler.setAcceptStrategy(AcceptStrategies.strictModelAccept());

        samplerTwitter = new DefaultSampler<>(modelTwitter, objective, explorers, stopAtMaxModelScore);
        samplerTwitter.setSamplingStrategy(SamplingStrategies.greedyModelStrategy());
        samplerTwitter.setAcceptStrategy(AcceptStrategies.strictModelAccept());

        trainer = new Trainer();
    }

    public void run() {
        log.info("Start JSON-Document disambiguation service.");

        Spark.post("/bire", "application/json", (request, response) -> {
            String jsonDocument = request.body();
            Document document = GerbilUtil.json2bire(jsonDocument, true);
            Document annotatedDocument = disambiguate(document);

//                        Document annotatedDocument = new Document(document.getDocumentContent(), document.getDocumentName());
//                        annotatedDocument.setAnnotations(document.getGoldStandard());
//                        annotatedDocument.setGoldStandard(document.getGoldStandard());
            response.type("application/json");

            return GerbilUtil.bire2json(annotatedDocument, false);
        });
    }

    public Document disambiguate(Document document) {
        log.info("####################");
        log.info("Request to disambiguate document:\n%s", document);

        if (document.getGoldStandard().isEmpty()) {
            log.info("Document has no annotations. Done!");
            return document;
        }

        List<Document> test = DocumentUtils.splitDocument(document, capacity);
        log.info("Split into %s smaller documents.", test.size());

        log.info("Predict ...");

        List<State> testResults = new ArrayList<>();

        if (document.getGoldStandard().size() > 3) {
            testResults = trainer.predict(sampler, testInitializer, test);
        } else {
            testResults = trainer.predict(samplerTwitter, testInitializer, test);
        }

        Document annotatedDocument = new Document(document.getDocumentContent(), document.getDocumentName());
        for (State state : testResults) {
            List<Annotation> a = annotatedDocument.getAnnotations();
            a.addAll(state.getEntities());
        }

        HashMap<String, Annotation> coveredAnnotations = new HashMap<>();
        HashMap<String, Annotation> labelAnnotations = new HashMap<>();

        for (Annotation a1 : annotatedDocument.getAnnotations()) {
            coveredAnnotations.put(a1.getStartIndex() + "," + a1.getEndIndex(), a1);
            labelAnnotations.put(a1.getWord(), a1);
            a1.setLink(DBPEDIA_LINK_PREFIX + "" + a1.getLink());
        }

        for (Annotation a1 : document.getGoldStandard()) {
            String s = a1.getStartIndex() + "," + a1.getEndIndex();

            if (!coveredAnnotations.containsKey(s)) {
                if (labelAnnotations.containsKey(a1.getWord())) {

                    a1.setLink(labelAnnotations.get(a1.getWord()).getLink());

                } else {
                    a1.setLink("http://de.citec.sc.ned/" + a1.getWord());

                }
                annotatedDocument.addAnnotation(a1);
            }
        }

        //surname - name postprocessing
        for (Annotation a1 : document.getAnnotations()) {
            for (Annotation a2 : document.getAnnotations()) {

                if (!a1.equals(a2)) {
                    if (a1.getWord().toLowerCase().contains(a2.getWord().toLowerCase()) && a1.getWord().length() > a2.getWord().length() && a1.getEndIndex() < a2.getEndIndex()) {
                        Set<String> classes = DBpediaEndpoint.getClasses(a1.getLink());
                        if (classes.contains("Person")) {
                            a2.setLink(a1.getLink());
                            break;
                        }
                    }
                }
            }
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
                return new TermFrequencyTemplate(useBins);
            case "PageRankTemplate":
                return new PageRankTemplate(useBins);
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
