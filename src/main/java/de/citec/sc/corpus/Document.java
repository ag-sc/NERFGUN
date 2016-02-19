/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import corpus.LabeledInstance;

import corpus.LabeledInstance;

/**
 *
 * @author sherzod
 */
public class Document implements LabeledInstance<List<Annotation>> {

    private String documentContent;

    private List<Annotation> goldStandard;
    private List<Annotation> annotations;

    private String documentName;

    public Document(String documentContent, String docName) {
        this.documentContent = documentContent;
        this.documentName = docName;
    }

    public String getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(String documentContent) {
        this.documentContent = documentContent;
    }

    public List<Annotation> getGoldStandard() {
        return goldStandard;
    }

    public void setGoldStandard(List<Annotation> goldStandard) {
        this.goldStandard = goldStandard;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.documentContent);
        hash = 53 * hash + Objects.hashCode(this.goldStandard);
        hash = 53 * hash + Objects.hashCode(this.documentName);
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
        final Document other = (Document) obj;
        if (!Objects.equals(this.documentContent, other.documentContent)) {
            return false;
        }
        if (!Objects.equals(this.goldStandard, other.goldStandard)) {
            return false;
        }
        if (!Objects.equals(this.documentName, other.documentName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String a = "Doc name: " + documentName + "\nContent:\n" + documentContent;

        a += "\n\nAnnotations:\n";
        for (Annotation a1 : annotations) {
            a += a1.toString() + "\n";
        }

        a += "\n\nGoldSet:\n";
        for (Annotation a1 : goldStandard) {
            a += a1.toString() + "\n";
        }
        return a;
    }

	// @Override
    // public String toString() {
    // return "Document [documentContent=" + documentContent + ", goldStandard="
    // + goldStandard + ", annotations="
    // + annotations + ", documentName=" + documentName + "]";
    // }
    @Override
    public List<Annotation> getGoldResult() {
        return goldStandard;
    }

    public void addAnnotation(Annotation a) {
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }
        this.annotations.add(a);
    }

}
