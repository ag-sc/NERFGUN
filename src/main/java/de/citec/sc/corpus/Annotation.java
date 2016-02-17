/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.corpus;

import java.util.Objects;

/**
 *
 * @author sherzod
 */
public class Annotation {
    private String word;
    private String link;
    private int startIndex, endIndex;

    /**
     * @param word
     * @param link
     * @param startPosition 
     */
    public Annotation(String word, String link, int startIndex, int endIndex) {
        this.word = word;
        this.link = link;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public Annotation(String word, String link) {
        this.word = word;
        this.link = link;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    

    public void setWord(String word) {
        this.word = word;
    }

    public void setLink(String link) {
        this.link = link;
    }



    public String getWord() {
        return word;
    }

    public String getLink() {
        return link;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.word);
        hash = 53 * hash + Objects.hashCode(this.link);
        hash = 53 * hash + this.startIndex;
        hash = 53 * hash + this.endIndex;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Annotation other = (Annotation) obj;
        if (!Objects.equals(this.word, other.word)) {
            return false;
        }
        if (!Objects.equals(this.link, other.link)) {
            return false;
        }
        if (this.startIndex != other.startIndex) {
            return false;
        }
        if (this.endIndex != other.endIndex) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "word=" + word + ", link=" + link + ", startIndex=" + startIndex + ", endIndex=" + endIndex;
    }
    


    public Annotation clone(){
        return new Annotation(word, link, startIndex, endIndex);
    }

    
    
    
}
