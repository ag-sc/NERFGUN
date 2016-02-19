/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.util.Objects;

import de.citec.sc.variables.State;
import utility.VariableID;
import variables.AbstractVariable;

/**
 *
 * @author sherzod
 */
public class Annotation extends AbstractVariable<State> {

	private String word;
	private String link;
	private int startIndex, endIndex;
	private int indexRank = -1;

	public Annotation(String word, String link, int startIndex, int endIndex, VariableID vID) {
		super(vID);
		this.word = word;
		this.link = link;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	/**
	 * @param word
	 * @param link
	 * @param startPosition
	 */
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

	public int getIndexRank() {
		return indexRank;
	}

	public void setIndexRank(int indexRank) {
		this.indexRank = indexRank;
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
		return "[" + id + ": " + link + "] \"" + word + "\" (" + startIndex + " - " + endIndex + ")";
	}

	public Annotation clone() {
		return new Annotation(word, link, startIndex, endIndex, id);
	}

}
