/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.query;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sherzod
 */
public class Search {

	private DBpediaRetriever dbpediaRetriever;
	private AnchorRetriever anchorRetriever;

	private boolean loadToMemory;

	public Search(boolean loadToMemory, String dbpediaIndexPath) {
		this.loadToMemory = loadToMemory;

		this.dbpediaRetriever = new DBpediaRetriever(dbpediaIndexPath, loadToMemory);
		this.anchorRetriever = new AnchorRetriever("anchorIndex", loadToMemory);

	}

	/**
	 * @return returns all resources that match the label using DBpedia
	 * @param searchTerm
	 * @param topK
	 */
	public List<String> getResourcesFromDBpedia(String searchTerm, int topK) {

		return dbpediaRetriever.getResources(searchTerm, topK, false);
	}

	/**
	 * @return returns all resources that match the label using DBpedia
	 * @param searchTerm
	 * @param topK
	 */
	public List<String> getResourcesFromAnchors(String searchTerm, int topK) {
		List<String> result = new ArrayList<>();

		for (String e1 : anchorRetriever.getResources(searchTerm, topK)) {
			try {
				result.add(URLDecoder.decode(e1, "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return result;
	}

	/**
	 * @return returns all resources that match the label using DBpedia and
	 *         Anchors
	 * @param searchTerm
	 * @param topK
	 */
	public List<String> getAllResources(String searchTerm, int topK) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		List<String> anchor = anchorRetriever.getResources(searchTerm, topK);
		List<String> dbpedia = dbpediaRetriever.getResources(searchTerm, topK, false);

		if (anchor.size() < dbpedia.size()) {
			int a = Math.min(topK / 2, anchor.size());
			int b = topK - a;
			result.addAll(anchor.subList(0, a));
			result.addAll(dbpedia.subList(0, Math.min(b, dbpedia.size())));
		} else {
			int a = Math.min(topK / 2, dbpedia.size());
			int b = topK - a;
			result.addAll(anchor.subList(0, Math.min(b, anchor.size())));
			result.addAll(dbpedia.subList(0, a));
		}
		return new ArrayList<>(result);
	}
}
