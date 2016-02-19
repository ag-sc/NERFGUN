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
		List<String> result = new ArrayList<>();

		// Set<String> searchTermSet = new LinkedHashSet<>();
		// searchTermSet.add(searchTerm);

		// for (String term : searchTermSet) {
		// get all from DBpedia
		// for (String e1 : dbpediaRetriever.getResources(searchTerm, topK,
		// false)) {
		// try {
		result.addAll(dbpediaRetriever.getResources(searchTerm, topK, false));
		// result.add(URLDecoder.decode(e1, "UTF-8"));
		// } catch (UnsupportedEncodingException ex) {
		// Logger.getLogger(Search.class.getName()).log(Level.SEVERE,
		// null, ex);
		// }
		// }
		// }

		// if (result.size() > topK) {
		// List<String> temp = new ArrayList<>();
		// temp.addAll(result);
		//
		// result.clear();
		//
		// result.addAll(temp.subList(0, topK));
		// }

		return result;
	}

	/**
	 * @return returns all resources that match the label using DBpedia
	 * @param searchTerm
	 * @param topK
	 */
	public List<String> getResourcesFromAnchors(String searchTerm, int topK) {
		List<String> result = new ArrayList<>();

		// Set<String> searchTermSet = new LinkedHashSet<>();
		// searchTermSet.add(searchTerm);

		// for (String term : searchTermSet) {
		// get all from Anchor
		for (String e1 : anchorRetriever.getResources(searchTerm, topK)) {
			try {

				result.add(URLDecoder.decode(e1, "UTF-8"));

			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(Search.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		// }

		// if (result.size() > topK) {
		// List<String> temp = new ArrayList<>();
		// temp.addAll(result);
		//
		// result.clear();
		//
		// result.addAll(temp.subList(0, topK));
		// }

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
		List<String> anchor = anchorRetriever.getResources(searchTerm, (topK + 1) / 2);
		List<String> dbpedia = dbpediaRetriever.getResources(searchTerm, topK / 2, false);

		// if (anchor.size() <= (topK + 1) / 2) {
		// result.addAll(anchor);
		// result.addAll(dbpedia.subList(0, result.size()));
		// }
		result.addAll(anchor);
		result.addAll(dbpedia);
		return new ArrayList<>(result);
	}
}
