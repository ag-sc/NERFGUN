/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.sc.corpus;

/**
 *
 * @author sherzod
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import corpus.Corpus;

public class DefaultCorpus implements Corpus<Document> {

	private List<Document> documents = new ArrayList<>();

	@Override
	public List<Document> getDocuments() {
		return documents;
	}

	@Override
	public void addDocument(Document doc) {
		this.documents.add(doc);
	}

	@Override
	public void addDocuments(Collection<Document> documents) {
		this.documents.addAll(documents);
	}

	@Override
	public String toString() {
		return toDetailedString();
	}

	public String toDetailedString() {
		StringBuilder builder = new StringBuilder();
		for (Document doc : documents) {
			builder.append(doc.toString());
			builder.append("\n");
		}
		return "DefaultCorpus [ #documents=" + documents.size() + ", documents=\n"
				+ builder.toString() + "]";
	}

}