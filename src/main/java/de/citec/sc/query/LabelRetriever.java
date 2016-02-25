package de.citec.sc.query;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

public abstract class LabelRetriever {

    protected final Comparator<Instance> frequencyComparator = new Comparator<Instance>() {

        @Override
        public int compare(Instance s1, Instance s2) {

            if (s1.getScore() > s2.getScore()) {
                return -1;
            } else if (s1.getScore() < s2.getScore()) {
                return 1;
            }

            return 0;
        }
    };

    protected Comparator<Instance> comparator = frequencyComparator;

    /**
     * returns top k URIs that match the searchTerm directly (not partial match)
     * returns top k URIs in sorted order by frequency if frequency isn't
     * available, sorting is done based on string similarity between searchTerm
     * and retrieved URI
     *
     * @return Set<Instace>
     * @param searchTerm
     * @param queryPart
     * @param retrievalPart
     * @param k
     */
    protected List<Instance> getDirectMatches(String searchTerm, String searchField, String returnField, int k, Directory indexDirectory) {
        List<Instance> instances = new ArrayList<>();

        try {

            searchTerm = searchTerm.toLowerCase();
            //Query q = new QueryParser("label", analyzer).parse(label);
            Query q = new TermQuery(new Term(searchField, searchTerm));

            // 3. search
            int hitsPerPage = 1000;
            IndexReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
//            TopDocs topdocs = searcher.search(q, k);
//            ScoreDoc[] hits = topdocs.scoreDocs;

            // 4. display results
            //System.out.println("Found " + hits.length + " hits.");
            HashMap<String, Integer> resultMap = new HashMap<>();

            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                //float score = hits[i].score;
                
                String p = d.get(returnField);
                int freq = 0;
                if (d.get("freq") != null) {
                    freq = Integer.parseInt(d.get("freq"));
                } else {
                    freq = 1;
                }

                
                if (freq > 0) {
                    if (resultMap.containsKey(p)) {
                        resultMap.put(p, resultMap.get(p) + freq);
                    } else {
                        resultMap.put(p, freq);
                    }

                }
            }

            int sum = 0;
            for (Integer i : resultMap.values()) {
                sum += i;
            }

            
            for (String r1 : resultMap.keySet()) {
                double score = (double) resultMap.get(r1) / (double) sum;
                Instance i = new Instance(r1, score);
                instances.add(i);
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        Collections.sort(instances, comparator);

        if (instances.size() > k) {
            instances = instances.subList(0, k);
        }

        return instances;
    }

    /**
     * returns top k URIs that match the searchTerm partially and directly
     * returns top k URIs in sorted order by frequency if frequency isn't
     * available, sorting is done based on string similarity between searchTerm
     * and retrieved URI
     *
     * @return Set<Instace>
     * @param searchTerm
     * @param queryPart
     * @param retrievalPart
     * @param k
     */
    protected List<Instance> getPartialMatches(String searchTerm, String searchField, String returnField, int k, Directory indexDirectory, StandardAnalyzer analyzer) {
        List<Instance> instances = new ArrayList<>();

        try {

            searchTerm = searchTerm.toLowerCase();
            Query q = new QueryParser(searchField, analyzer).parse(searchTerm);

            // 3. search
            int hitsPerPage = 1000;
            IndexReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
//            TopDocs topdocs = searcher.search(q, k);
//            ScoreDoc[] hits = topdocs.scoreDocs;

            // 4. display results
            //System.out.println("Found " + hits.length + " hits.");
            HashMap<String, Integer> resultMap = new HashMap<>();
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                //float score = hits[i].score;

                String p = d.get(returnField);
                int freq = 0;
                if (d.get("freq") != null) {
                    freq = Integer.parseInt(d.get("freq"));
                } else {
                    freq = 1;
                }

                if (freq > 0) {
                    if (resultMap.containsKey(p)) {
                        resultMap.put(p, resultMap.get(p) + freq);
                    } else {
                        resultMap.put(p, freq);
                    }

                }
            }

            int sum = 0;
            for (Integer i : resultMap.values()) {
                sum += i;
            }

            
            for (String r1 : resultMap.keySet()) {
                double score = (double) resultMap.get(r1) / (double) sum;
                Instance i = new Instance(r1, score);
                instances.add(i);
            }

            reader.close();

        } catch (Exception e) {
            //e.printStackTrace();
        }

        Collections.sort(instances, comparator);

        if (instances.size() > k) {
            instances = instances.subList(0, k);
        }

        return instances;
    }

    
}
