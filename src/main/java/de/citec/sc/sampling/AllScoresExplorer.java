package de.citec.sc.sampling;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.query.CandidateRetriever;
import de.citec.sc.query.Instance;
import de.citec.sc.templates.IndexMapping;
import de.citec.sc.variables.State;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sampling.Explorer;

/**
 *
 * @author sjebbara
 */
public class AllScoresExplorer implements Explorer<State> {

    private static Logger log = LogManager.getFormatterLogger();
    private static int maxNumberOfCandidateURIs;
    private static CandidateRetriever index;
    public static Map<Integer, Double> pageRankMap;
    private boolean isInitialized = false;

    public AllScoresExplorer(CandidateRetriever index) {
        this(index, 100);

    }

    public AllScoresExplorer(CandidateRetriever index, int maxNumberOfCandidateURIs) {
        super();
        this.index = index;
        this.maxNumberOfCandidateURIs = maxNumberOfCandidateURIs;
        if (!isInitialized) {
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
            double sumPR = 0.0;

            List<Instance> filteredCandidates = new ArrayList<>();

            for (Instance i : candidateURIs) {

                double d = 0.0;
                try {
                    Integer pID = IndexMapping.indexMappings.get(i.getUri());

                    if (pID != null) {
                        Double d1 = pageRankMap.get(pID);
                        if (d1 != null) {
                            d = d1;
                        }
                    } else {
                        filteredCandidates.add(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                sumPR += d;

            }

            //remove candidates with zero pagerank score
            candidateURIs.removeAll(filteredCandidates);

            for (int i = 0; i < candidateURIs.size(); i++) {
                Instance candidateURI = candidateURIs.get(i);// .replace("http://dbpedia.org/resource/",
                // "");
                State generatedState = new State(currentState);
                Annotation modifiedAnntation = generatedState.getEntity(a.getID());
                modifiedAnntation.setLink(candidateURI.getUri());
                modifiedAnntation.setIndexRank(i);

                // Relative Term Freq
                modifiedAnntation.setRelativeTermFrequencyScore(candidateURI.getScore());

                // PageRank Score
                double d = 0.0;
                try {
                    Integer pID = IndexMapping.indexMappings.get(candidateURI.getUri());

                    if (pID != null) {
                        Double d1 = pageRankMap.get(pID);
                        if (d1 != null) {
                            d = d1;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (sumPR != 0) {
                    modifiedAnntation.setPageRankScore(d / sumPR);
                    if (d == 0.0) {
                        System.out.println("Sum Not Zero " + modifiedAnntation);
                    }
                } else {
                    System.out.println("Sum Zero " + modifiedAnntation);
                }

                generatedStates.add(generatedState);
            }
        }
        log.debug("Total number of %s states generated.", generatedStates.size());
        return generatedStates;
    }

    private void loadPageRanks() {

        String path = "pagerank.csv";

        System.out.print("Loading pagerank scores to memory ... ");

        if (pageRankMap == null) {
            
            pageRankMap = new ConcurrentHashMap<>(19500000);
            try {
                FileInputStream fstream = new FileInputStream(path);
                BufferedReader indexMappingReader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));

                //BufferedReader indexMappingReader = new BufferedReader(new FileReader(new File(keyFiles)));
                String line = "";
                while ((line = indexMappingReader.readLine()) != null) {
                    String[] data = line.split("\t");
                    String uri = data[1];

                    Double v = Double.parseDouble(data[2]);

                    if (!(uri.contains("Category:") || uri.contains("(disambiguation)") || uri.contains("File:"))) {

                        uri = StringEscapeUtils.unescapeJava(uri);

                        try {
                            uri = URLDecoder.decode(uri, "UTF-8");
                        } catch (Exception e) {
                        }

                        Integer key = IndexMapping.indexMappings.get(uri);
                        if (key != null) {
                            pageRankMap.put(key, v);
                        }
//                    else {
//                        System.out.println(line);
//                    }
                    }
                }
                indexMappingReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("  DONE");
        }

//        try (Stream<String> stream = Files.lines(Paths.get(path))) {
//            stream.parallel().forEach(item -> {
//
//                String line = item.toString();
//
//                String[] data = line.split("\t");
//                String uri = data[1];
//
//                Double v = Double.parseDouble(data[2]);
//
//                if (!(uri.contains("Category:") || uri.contains("(disambiguation)") || uri.contains("File:"))) {
//
//                    uri = StringEscapeUtils.unescapeJava(uri);
//
//                    try {
//                        uri = URLDecoder.decode(uri, "UTF-8");
//                    } catch (Exception e) {
//                    }
//
//                    Integer key = IndexMapping.indexMappings.get(uri);
//                    if (key != null) {
//                        pageRankMap.put(key, v);
//                    }
//                    else{
//                        System.err.println(uri + " "+key);
//                    }
//
//                }
//
//            });
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        isInitialized = true;
    }

    private void savePageRankMap() {
        try {

            FileOutputStream fout = new FileOutputStream("serializedPageRankMap.bin");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(pageRankMap);
            oos.close();
            System.out.println("Saved page rank map");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readPageRankMap() {
        try {

            FileInputStream fin = new FileInputStream("serializedPageRankMap.bin");
            ObjectInputStream ois = new ObjectInputStream(fin);
            pageRankMap = (Map) ois.readObject();
            ois.close();

        } catch (Exception ex) {
            System.err.println("File not found serializedPageRankMap.bin");
        }
    }
    
    

}
