/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.corpus;

import java.util.List;

/**
 *
 * @author sherzod
 */
public class Corpus {
    private List<Document> documents;
    private String corpusName;

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public String getCorpusName() {
        return corpusName;
    }

    public void setCorpusName(String corpusName) {
        this.corpusName = corpusName;
    }

    @Override
    public String toString() {
        String s =  "Corpus " + corpusName+ "\n";
        
        for(Document d : documents){
            s+= d.toString()+"\n\n";
        }
        
        return s;
    }
    
    
}
