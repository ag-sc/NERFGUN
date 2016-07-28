/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import java.text.DecimalFormat;
import java.util.Set;

import variables.AbstractVariable;

/**
 *
 * The Annotation contains now all links that can be assigned for that word.
 *
 * @author sherzod, hterhors
 */
public class Annotation extends AbstractVariable {

	private static DecimalFormat LUCENE_SCORE_FORMAT = new DecimalFormat("0.0000");
	public final static String DEFAULT_ID = "<EMPTY-URI>";
	private String word;
	private String link;
	private int startIndex, endIndex;
	private int indexRank = -1;
	private double relativeTermFrequencyScore;
	private double pageRankScore = 0.0;
	private double stringSimilarity;
	private Set<String> possibleLinks;

	public Annotation(Set<String> possibleLinks, String word, String link, int startIndex, int endIndex) {
		this.word = word;
		this.link = link;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.possibleLinks = possibleLinks;
	}

	public void setPossibleLinks(Set<String> possibleLinks) {
		this.possibleLinks = possibleLinks;
	}

	public Set<String> getPossibleLinks() {
		return possibleLinks;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + endIndex;
		result = prime * result + ((link == null) ? 0 : link.hashCode());
		result = prime * result + startIndex;
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Annotation other = (Annotation) obj;
		if (endIndex != other.endIndex)
			return false;
		if (link == null) {
			if (other.link != null)
				return false;
		} else if (!link.equals(other.link))
			return false;
		if (startIndex != other.startIndex)
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

	public String toString() {
		return "[" + indexRank + ".) " + link + "] \"" + word + "\" (" + startIndex + " - " + endIndex + ")";
	}

	public Annotation clone() {
		Annotation a = new Annotation(possibleLinks, word, link, startIndex, endIndex);
		a.setIndexRank(indexRank);
		a.setStringSimilarity(stringSimilarity);
		a.setPageRankScore(pageRankScore);
		a.setRelativeTermFrequencyScore(relativeTermFrequencyScore);
		return a;
	}

	public double getRelativeTermFrequencyScore() {
		return relativeTermFrequencyScore;
	}

	public void setRelativeTermFrequencyScore(double relativeTermFrequencyScore) {
		this.relativeTermFrequencyScore = relativeTermFrequencyScore;
	}

	public double getPageRankScore() {
		return pageRankScore;
	}

	public void setPageRankScore(double pageRankScore) {
		this.pageRankScore = pageRankScore;
	}

	public double getStringSimilarity() {
		return stringSimilarity;
	}

	public void setStringSimilarity(double stringSimilarity) {
		this.stringSimilarity = stringSimilarity;
	}

}
