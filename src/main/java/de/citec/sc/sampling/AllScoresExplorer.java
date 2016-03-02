package de.citec.sc.sampling;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.Instance;
import de.citec.sc.similarity.measures.SimilarityMeasures;
import de.citec.sc.variables.State;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringEscapeUtils;
import sampling.Explorer;

/**
 *
 * @author sjebbara
 */
public class AllScoresExplorer implements Explorer<State> {

    private static Logger log = LogManager.getFormatterLogger();
    private int maxNumberOfCandidateURIs;
    private CandidateRetriever index;
    ConcurrentHashMap<String, Double> pageRankMap;

    public AllScoresExplorer(CandidateRetriever index) {
        this(index, 100);

    }

    public AllScoresExplorer(CandidateRetriever index, int maxNumberOfCandidateURIs) {
        super();
        this.index = index;
        this.maxNumberOfCandidateURIs = maxNumberOfCandidateURIs;
        if ((pageRankMap == null) || (pageRankMap.isEmpty())) {
            loadPageRanks();
        }
    }

    @Override
    public List<State> getNextStates(State currentState) {
        log.debug("Generate successor states for state:\n%s", currentState);
        List<State> generatedStates = new ArrayList<>();
        for (Annotation a : currentState.getEntities()) {
            log.debug("Generate successor states for annotation:\n%s", a);
            String annotationText = a.getWord();
            // String annotationText =
            // currentState.getDocument().getDocumentContent().substring(a.getStartIndex(),a.getEndIndex());
            List<Instance> candidateURIs = index.getAllResources(annotationText, maxNumberOfCandidateURIs);

            log.debug("%s candidates retreived.", candidateURIs.size());
            double sumPR = 0;
            double sumEditDistance = 0;

            for (Instance i : candidateURIs) {
                Double d = pageRankMap.get(i.getUri());
                if (d == null) {
                    d = 0.0;
                }
                sumPR += d;

                try {
                    final String link = i.getUri().replaceAll("_", " ").toLowerCase();
                    final String word = a.getWord().toLowerCase();

                    final int levenDist = SimilarityMeasures.levenshteinDistance(link, word);

                    final int max = Math.max(link.length(), word.length());

                    final double editDistance = ((double) (max - levenDist) / (double) max);

                    sumEditDistance += editDistance;
                } catch (Exception e) {

                }
            }

            for (int i = 0; i < candidateURIs.size(); i++) {
                Instance candidateURI = candidateURIs.get(i);// .replace("http://dbpedia.org/resource/",
                // "");
                State generatedState = new State(currentState);
                Annotation modifiedAnntation = generatedState.getEntity(a.getID());
                modifiedAnntation.setLink(candidateURI.getUri());
                modifiedAnntation.setIndexRank(i);

                //Relative Term Freq
                modifiedAnntation.setRelativeTermFrequencyScore(candidateURI.getScore());

                //PageRank Score
                Double d = pageRankMap.get(candidateURI.getUri());
                if (d == null) {
                    d = 0.0;
                }
                modifiedAnntation.setPageRankScore(d / sumPR);

//                //String similarity score
                try {
                    final String link = modifiedAnntation.getLink().replaceAll("_", " ").toLowerCase();
                    final String word = modifiedAnntation.getWord().toLowerCase();

                    final int levenDist = SimilarityMeasures.levenshteinDistance(link, word);

                    final int max = Math.max(link.length(), word.length());

                    final double editDistance = ((double) (max - levenDist) / (double) max);
                    modifiedAnntation.setStringSimilarity(editDistance / sumEditDistance);
                } catch (Exception e) {
                }
                generatedStates.add(generatedState);
            }
        }
        log.debug("Total number of %s states generated.", generatedStates.size());
        return generatedStates;
    }

    private void loadPageRanks() {

        pageRankMap = new ConcurrentHashMap<>(19500000);
        String path = "pagerank.csv";

        System.out.print("Loading pagerank scores to memory ... ");

        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            stream.parallel().forEach(item -> {

                String line = item.toString();

                String[] data = line.split("\t");
                String uri = data[1];
                Double v = Double.parseDouble(data[2]);
                if (!(uri.contains("Category:") || uri.contains("(disambiguation)"))) {

                    uri = StringEscapeUtils.unescapeJava(uri);

                    try {
                        uri = URLDecoder.decode(uri, "UTF-8");
                    } catch (Exception e) {
                    }

                    pageRankMap.put(uri, v);

                }

            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("  DONE");
    }

}
