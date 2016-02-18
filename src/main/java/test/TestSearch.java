/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.DefaultCorpus;
import de.citec.sc.corpus.Document;
import de.citec.sc.evaluator.Evaluator;
import de.citec.sc.query.Search;

/**
 *
 * @author sherzod
 */
public class TestSearch {
    public static void main(String[] args) {
        Search indexSearch  = new Search(false, "dbpediaIndexAll");
        
        System.out.println(indexSearch.getAllResources("bielefeld", 10));
        
        CorpusLoader loader = new CorpusLoader();
        DefaultCorpus c = loader.loadCorpus(CorpusLoader.CorpusName.CoNLL);
        
        Evaluator ev = new Evaluator();
        Document d = c.getDocuments().get(0);
        Annotation a1 = new Annotation("German", "http://en.wikipedia.org/wiki/Germany", 11, 17, null);
        d.addAnnotation(a1);
        System.out.println(ev.evaluate(d));
    }
}
